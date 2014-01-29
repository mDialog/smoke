package smoke.test

import smoke.Request

import java.net.URI

case class TestRequest(uriString: String,
    method: String = "GET",
    headers: Seq[(String, String)] = Seq.empty,
    body: String = "",
    keepAlive: Boolean = true,
    requestIp: String = "0.0.0.0") extends Request {

  val version = "HTTP/1.1"
  val uri = new URI(uriString)

  val cookies: Map[String, String] =
    allHeaderValues("Cookie")
      .mkString(";") // Concatenate everything together in case one "Cookie" value defined several key=value; key=value
      .split(";")
      .map(_.split("=").toList)
      .collect {
        case List(key, value) ⇒ key -> value
      }
      .toMap

  val contentLength = body.getBytes.length
}
