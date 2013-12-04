package smoke.examples

import smoke._
import com.typesafe.config.ConfigFactory

class RequestHandlerException extends Exception
class FutureResultException extends Exception
class BeforeException extends Exception
class AfterException extends Exception

object ErrorHandlerExampleApp extends App {
  val smoke = new ErrorHandlerExampleSmoke
}

class ErrorHandlerExampleSmoke extends Smoke {
  val config = ConfigFactory.load().getConfig("smoke")
  val executionContext = scala.concurrent.ExecutionContext.global

  before {
    case GET(Path("/before-exception")) ⇒
      throw new BeforeException
    case request ⇒ request
  }

  onRequest {
    case GET(Path("/future-result-error")) ⇒ reply {
      throw new FutureResultException
      Response(Ok, body = "Hello world")
    }

    case GET(Path("/request-handler-error")) ⇒
      throw new RequestHandlerException
      reply(Response(Ok, body = "Hello world"))

    case GET(Path("/after-exception")) ⇒
      reply(Response(Ok, body = "raise havoc"))

    case _ ⇒ reply(Response(NotFound))
  }

  onError {
    case e: FutureResultException ⇒
      Response(InternalServerError, body = "Future result exception")
    case e: RequestHandlerException ⇒
      Response(InternalServerError, body = "Request handler exception")
    case e: BeforeException ⇒
      Response(InternalServerError, body = "Before filter exception")
    case e: AfterException ⇒
      Response(InternalServerError, body = "After filter exception")
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

