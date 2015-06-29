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
  val smokeConfig: Config
  implicit val executionContext: ExecutionContext

  private lazy val server = new NettyServer()(smokeConfig, executionContext)

  private var running = false

  private var beforeFilter = { request: Request ⇒ request }
  private var responder = { request: Request ⇒
    Future.successful(Response(ServiceUnavailable))
  }
  private var afterFilter = { response: Response ⇒ response }
  private var errorHandler: PartialFunction[(Request, Throwable), Response] = {
    case (r, t: Throwable) ⇒ Response(InternalServerError, body = t.getMessage + "\n" +
      t.getStackTrace.mkString("\n"))
  }

  private var shutdownHooks = List(() ⇒ {
    server.stop()
  })

  private def withErrorHandling(errorProne: smoke.Request ⇒ Future[Response]) = {
    case class RequestHandlerException(r: smoke.Request, e: Throwable) extends Exception("", e) {
      def asTuple: (smoke.Request, Throwable) = (r, e)
    }

    def maybeFails(x: smoke.Request): Future[Response] = {
      try {
        errorProne(x) recoverWith encapsulate(x)
      } catch encapsulate(x)
    }

    def encapsulate(x: smoke.Request): PartialFunction[Throwable, Future[Response]] = {
      case t: Throwable ⇒ fail(RequestHandlerException(x, t))
    }

    val decapsulate: PartialFunction[Throwable, (smoke.Request, Throwable)] = {
      case rhe: RequestHandlerException ⇒ rhe.asTuple
    }

    def tryAfterFilter(x: Response): Response =
      try { afterFilter(x) } catch { case _: Throwable ⇒ x }

    maybeFails _ andThen {
      _ recover (decapsulate andThen errorHandler andThen tryAfterFilter)
    }
  }

  def application = withErrorHandling {
    beforeFilter andThen responder andThen { _ map afterFilter }
  }

  def before(filter: (Request) ⇒ Request) { beforeFilter = filter }

  def after(filter: (Response) ⇒ Response) { afterFilter = filter }

  def onRequest(handler: (Request) ⇒ Future[Response]) { responder = handler }

  def onError(handler: PartialFunction[(Request, Throwable), Response]) {
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