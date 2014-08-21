package smoke

import com.typesafe.config._
import scala.concurrent.{ Future, ExecutionContext }
import smoke.netty.NettyServer

trait Smoke {
  val smokeConfig: Config
  def executionContext: ExecutionContext

  private lazy val server = new NettyServer()(smokeConfig, executionContext)

  private var running = false

  private def withErrorHandling(errorProne: smoke.Request ⇒ Future[Response]) = {
    implicit val ec = executionContext
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

    maybeFails _ andThen { _ recover (decapsulate andThen onError) }
  }

  private[smoke] def application = {
    implicit val ec = executionContext
    withErrorHandling {
      before andThen onRequest andThen { _ map after }
    }
  }

  def before: PartialFunction[Request, Request] = { case request: Request ⇒ request }

  def onRequest: PartialFunction[Request, Future[Response]]

  def after: PartialFunction[Response, Response] = { case response: Response ⇒ response }

  def onError: PartialFunction[(Request, Throwable), Response] = {
    case (r, t: Throwable) ⇒
      Response(InternalServerError, body = s"${t.getMessage}\n " + t.getStackTrace.mkString("\n"))
  }

  def beforeShutdown {}

  def afterShutdown {}

  def reply(action: ⇒ Response) = Future(action)(executionContext)

  def fail(e: Exception) = Future.failed(e)

  def shutdownHooks = {
    beforeShutdown
    server.stop()
    afterShutdown
  }

  def shutdown() {
    if (running) {
      running = false
      shutdownHooks
    }
  }

  def start() {
    if (!running) try {
      server.setApplication(application)
      server.start()
      running = true
    } catch {
      case e: Throwable ⇒
        shutdownHooks
        throw e
    }
  }

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    def run = shutdown()
  }))
}