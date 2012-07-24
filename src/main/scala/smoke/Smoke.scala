package smoke

import scala.compat.Platform.currentTime
import com.typesafe.config.ConfigFactory

import akka.dispatch.{ Future, Promise, ExecutionContext }
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.util.duration._

import smoke.netty.NettyServer

trait Smoke extends DelayedInit {
  implicit val config = ConfigFactory.load()

  implicit var system: ActorSystem = _
  implicit var dispatcher: ExecutionContext = _
  implicit var timeout: Timeout = _
  var server: Server = _

  def init() {
    system = ActorSystem("Smoke", config)
    dispatcher = system.dispatcher

    val timeoutDuration: Long = config.getMilliseconds("smoke.timeout")
    timeout = Timeout(timeoutDuration milliseconds)

    server = new NettyServer
  }

  private var beforeFilter = { request: Request ⇒ request }
  private var responder = { request: Request ⇒
    Promise.successful(Response(ServiceUnavailable)).future
  }
  private var afterFilter = { response: Response ⇒ response }
  private var errorHandler: PartialFunction[Throwable, Response] = {
    case t: Throwable ⇒ Response(InternalServerError, body = t.getMessage + "\n" +
      t.getStackTrace.mkString("\n"))
  }

  private var shutdownHooks = List(() ⇒ {
    server.stop()
    system.shutdown()
  })

  def application =
    beforeFilter andThen responder andThen { f ⇒
      f recover (errorHandler) map afterFilter
    }

  def before(filter: (Request) ⇒ Request) { beforeFilter = filter }

  def after(filter: (Response) ⇒ Response) { afterFilter = filter }

  def onRequest(handler: (Request) ⇒ Future[Response]) { responder = handler }

  def onError(handler: PartialFunction[Throwable, Response]) {
    errorHandler = handler orElse errorHandler
  }

  def beforeShutdown(hook: ⇒ Unit) { shutdownHooks = hook _ :: shutdownHooks }

  def afterShutdown(hook: ⇒ Unit) { shutdownHooks = shutdownHooks ::: List(hook _) }

  def reply(action: ⇒ Response) = Future(action)

  def reply(r: Response) = Promise.successful(r)

  def fail(e: Exception) = Promise.failed(e)

  val executionStart: Long = currentTime
  var running = false

  protected def args: Array[String] = _args
  private var _args: Array[String] = _

  private var initCode: () ⇒ Unit = _
  override def delayedInit(body: ⇒ Unit) { initCode = (() ⇒ body) }

  def init(args: Array[String] = Seq.empty.toArray) {
    if (!running) {
      _args = args
      initCode()
      running = true
    }
  }

  def shutdown() {
    if (running) {
      running = false
      shutdownHooks foreach { hook ⇒ hook() }
    }
  }

  def main(args: Array[String]) = {
    init(args)

    server.setApplication(application)
    server.start()

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run = shutdown()
    }))
  }
}
