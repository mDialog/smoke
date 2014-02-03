package smoke.examples

import smoke._
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future

class RequestHandlerException extends Exception
class FutureResultException extends Exception
class BeforeException extends Exception
class AfterException extends Exception

object ErrorHandlerExampleApp extends App {
  val smoke = new ErrorHandlerExampleSmoke
}

class ErrorHandlerExampleSmoke extends Smoke {
  val smokeConfig = ConfigFactory.load().getConfig("smoke")
  implicit val executionContext = scala.concurrent.ExecutionContext.global

  before {
    case GET(Path("/before-exception")) ⇒
      throw new BeforeException
    case request ⇒ request
  }

  onRequest {
    case GET(Path("/future-result-error")) ⇒
      fail(new FutureResultException)

    case GET(Path("/request-handler-error")) ⇒
      throw new RequestHandlerException
      reply(Response(Ok, body = "Hello world"))

    case GET(Path("/after-exception")) ⇒
      reply(Response(Ok, body = "raise havoc"))

    case _ ⇒ reply(Response(NotFound))
  }

  onError {
    case (r, e: FutureResultException) ⇒
      Response(InternalServerError, body = "Future result exception while processing " + r.toShortString)
    case (r, e: RequestHandlerException) ⇒
      Response(InternalServerError, body = "Request handler exception while processing " + r.toShortString)
    case (r, e: BeforeException) ⇒
      Response(InternalServerError, body = "Before filter exception while processing " + r.toShortString)
    case (r, e: AfterException) ⇒
      Response(InternalServerError, body = "After filter exception while processing " + r.toShortString)
  }

  after {
    case Response(_, _, UTF8Data("raise havoc")) ⇒
      throw new AfterException

    case Response(status, headers, body) ⇒
      val _headers = headers ++ Map(
        "Server" -> "ErrorHandlerExampleApp/0.0.1",
        "Connection" -> "Close")

      Response(status, _headers, body)
  }
}

