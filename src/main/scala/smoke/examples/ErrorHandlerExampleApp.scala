package smoke.examples

import smoke._

object ErrorHandlerExampleApp extends Smoke {
  onRequest {
    case _ ⇒
      throw new Exception("oopsie")
      reply(Response(Ok, body = "Hello world"))
  }

  onError {
    case t: Throwable ⇒
      Response(InternalServerError, body = "My custom error response")
  }

  after { response ⇒
    val headers = response.headers ++ Map(
      "Server" -> "ErrorHandlerExampleApp/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }
}

