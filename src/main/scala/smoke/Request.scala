package smoke

import java.net.{ URI, URLDecoder }

trait Request extends Headers {
  val version: String
  val method: String
  val uri: URI
  val path: String
  val hostWithPort: String
  val host: String
  val port: Option[Int]
  val ip: String
  val keepAlive: Boolean
  val headers: Seq[(String, String)]
  val timestamp: Long = System.currentTimeMillis

  val queryString: Option[String]
  val contentType: Option[String]
  val userAgent: Option[String]

  val queryParams: Map[String, String]
  val formParams: Map[String, String]
  val params: Map[String, String]

  val cookies: Map[String, String]

  val body: String

  val contentLength: Int

  //Sort accept header by given priority (q=?)
  lazy val accept: Seq[String] =
    allHeaderValues("accept").map {
      _.split(";").toList match {
        case mt :: p :: Nil ⇒ (mt, p.split("q=").last.toFloat)
        case mt :: _        ⇒ (mt, 1.0f)
      }
    }.sortWith((a, b) ⇒ a._2 > b._2).map(_._1)

  override def toString = method + " - " + path + "-" + headers + "-" + "\n" + body

  protected def parseParams(params: String) =
    (params.split("&") map (_.split("=").toList) map {
      case name :: value :: Nil ⇒ Some((decode(name), decode(value)))
      case name :: Nil          ⇒ Some((decode(name), ""))
      case _                    ⇒ None
    }).toSeq.flatten toMap

  protected def decode(s: String) = URLDecoder.decode(s, "UTF-8")
}

object Request {
  private val trustedAddress = """^127\.0\.0\.1$|^10\..+$|^192\.168\..+$|^172\.1[6-9]\..+$|^172\.2[0-9]\..+$|^172\.3[0-1]\..+$|^::1$|^fd[0-9a-f]{2}:.+|^localhost$|^unix$""".r
  def isTrusted(address: String) = {
    trustedAddress.pattern.matcher(address).matches
  }
}
