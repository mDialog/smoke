package smoke

import com.typesafe.config._
import scala.concurrent.{ Future, ExecutionContext }
import smoke.netty.NettyServer

trait SmokeApp extends App with Smoke {
  override def delayedInit(body: ⇒ Unit) = {
    super[App].delayedInit(super[Smoke].delayedInit(body))
  }
}

trait Smoke extends DelayedInit {
  val config: Config
  implicit val executionContext: ExecutionContext

  private lazy val server = new NettyServer()(config, executionContext)

  private var running = false

  private var beforeFilter = { request: Request ⇒ request }
  private var responder = { request: Request ⇒
    Future.successful(Response(ServiceUnavailable))
  }
  private var afterFilter = { response: Response ⇒ response }
  private var errorHandler: PartialFunction[Throwable, Response] = {
    case t: Throwable ⇒ Response(InternalServerError, body = t.getMessage + "\n" +
      t.getStackTrace.mkString("\n"))
  }

  private var shutdownHooks = List(() ⇒ {
    server.stop()
  })

  private def withErrorHandling[T](errorProne: T ⇒ Future[Response]) = {
    def maybeFails(x: T): Future[Response] = {
      try {
        errorProne(x)
      } catch {
        case e: Exception ⇒
          Future.failed(e)
      }
    }

    maybeFails _ andThen { _ recover errorHandler }
  }

  def application = withErrorHandling {
    beforeFilter andThen responder andThen { _ map afterFilter }
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

  def fail(e: Exception) = Future.failed(e)

  def shutdown() {
    if (running) {
      running = false
      shutdownHooks foreach { hook ⇒ hook() }
    }
  }

  private[smoke] def init() {
    try {
      server.setApplication(application)
      server.start()
      running = true
    } catch {
      case e: Throwable ⇒
        shutdownHooks foreach { hook ⇒ hook() }
        throw e
    }
  }

  def delayedInit(body: ⇒ Unit) = {
    body
    init()
  }

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    def run = shutdown()
  }))
}