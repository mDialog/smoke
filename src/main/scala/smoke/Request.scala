package smoke

import java.net.{ URI, URLDecoder }

trait Request {
  val version: String
  val method: String
  val uri: URI
  val path: String
  val hostWithPort: String
  val host: String
  val port: Option[Int]
  val ip: String
  val keepAlive: Boolean
  val headers: Map[String, String]
  val timestamp: Long = System.currentTimeMillis

  val queryString: Option[String]
  val contentType: Option[String]
  val userAgent: Option[String]

  val queryParams: Map[String, String]
  val formParams: Map[String, String]
  val params: Map[String, String]

  val body: String

  val contentLength: Int

  override def toString = method + " - " + path + "-" + headers + "-" + "\n" + body

  protected def parseParams(params: String) =
    params.split("&") map (_.split("=")) map { p ⇒
      (decode(p.head), decode(p.last))
    } toMap

  protected def decode(s: String) = URLDecoder.decode(s, "UTF-8")
}

object Request {
  private val trustedAddress = """^127\.0\.0\.1$|^10\..+$|^192\.168\..+$|^172\.1[6-9]\..+$|^172\.2[0-9]\..+$|^172\.3[0-1]\..+$|^::1$|^fd[0-9a-f]{2}:.+|^localhost$|^unix$""".r
  def isTrusted(address: String) = {
    trustedAddress.pattern.matcher(address).matches
  }
}
