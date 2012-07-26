package smoke.examples

import smoke._

class RequestHandlerException extends Exception
class FutureResultException extends Exception

object ErrorHandlerExampleApp extends Smoke {

  onRequest {
    case GET(Path("/future-result-error")) ⇒ reply {
      throw new FutureResultException
      Response(Ok, body = "Hello world")
    }

    case GET(Path("/request-handler-error")) ⇒
      throw new RequestHandlerException
      reply(Response(Ok, body = "Hello world"))

    case _ ⇒ reply(Response(NotFound))
  }

  onError {
    case e: FutureResultException ⇒
      Response(InternalServerError, body = "Future result exception")
    case e: RequestHandlerException ⇒
      Response(InternalServerError, body = "Request handler exception")
  }

  after { response ⇒
    val headers = response.headers ++ Map(
      "Server" -> "ErrorHandlerExampleApp/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }

}

