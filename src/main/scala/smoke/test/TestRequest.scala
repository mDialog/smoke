package smoke.test

import smoke.Request

import java.net.URI

case class TestRequest(uriString: String,
    method: String = "GET",
    headers: Seq[(String, String)] = Seq.empty,
    body: String = "",
    keepAlive: Boolean = true) extends Request {
  val version = "HTTP/1.1"
  val uri = new URI(uriString)
  val path = uri.getPath
  val ip = "0.0.0.0"

  val host = uri.getHost
  val port = if (uri.getPort >= 0) Some(uri.getPort) else None
  val hostWithPort = host + (port map (":" + _.toString) getOrElse (""))

  val queryString: Option[String] = uri.getRawQuery match {
    case q: String ⇒ Some(q)
    case null      ⇒ None
  }

  val contentType: Option[String] = lastHeaderValue("Content-Type")
  val userAgent: Option[String] = lastHeaderValue("User-Agent")

  val queryParams: Map[String, String] = queryString match {
    case Some(string) ⇒ parseParams(string)
    case None         ⇒ Map.empty
  }

  val formParams: Map[String, String] = contentType match {
    case Some("application/x-www-form-urlencoded") ⇒ parseParams(body)
    case _                                         ⇒ Map.empty
  }

  val params: Map[String, String] = queryParams ++ formParams

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
