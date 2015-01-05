package smoke

import java.net.{ URI, URLDecoder }

trait Request extends Headers {
  val version: String
  val method: String
  val uri: URI
  val requestIp: String
  val keepAlive: Boolean
  val headers: Seq[(String, String)]
  val cookies: Map[String, String]
  val body: String
  val contentLength: Int

  val timestamp: Long = System.currentTimeMillis

  lazy val host = lastHeaderValue("host").
    map(_.split(":").head).
    getOrElse(uri.getHost)

  lazy val port: Option[Int] = {
    lastHeaderValue("host").
      flatMap { h ⇒
        h.split(":").toList match {
          case host :: port :: Nil ⇒ Some(port.toInt)
          case _                   ⇒ None
        }
      } orElse {
        uri.getPort match {
          case i if i > 0 ⇒ Some(i)
          case _          ⇒ None
        }
      }
  }

  lazy val hostWithPort = host + (port map (":" + _.toString) getOrElse (""))

  def path = uri.getPath

  def queryString = Option(uri.getRawQuery)

  lazy val queryParamsValues: Map[String, Seq[String]] = queryString.map(parseParams(_)).getOrElse(Map.empty[String, Seq[String]])

  lazy val queryParams = queryParamsValues.map { case (param, values) ⇒ (param -> values.head) }

  lazy val formParamsValues: Map[String, Seq[String]] = {
    contentType match {
      case Some(contentType) if contentType.startsWith("application/x-www-form-urlencoded") ⇒
        parseParams(body)
      case _ ⇒ Map.empty
    }
  }

  lazy val formParams: Map[String, String] = formParamsValues.map { case (param, values) ⇒ (param -> values.head) }

  lazy val paramsValues = queryParamsValues ++ formParamsValues

  lazy val params = queryParams ++ formParams

  lazy val ip = {
    // If there are multiple x-forwarded-for headers, we either concatenate them 
    // into a single ","-delimited String, or just pick the last one (since that's the value
    // in which we are interested)
    val xForwardedFor = lastHeaderValue("x-forwarded-for")

    val xForwardedForIps: Seq[String] = xForwardedFor match {
      case Some(ips) ⇒ ips.split(",")
        .map(h ⇒ h.trim)
        .filter(h ⇒ !Request.isTrusted(h))
        .toSeq
      case None ⇒ Seq.empty
    }

    xForwardedForIps.isEmpty match {
      case true  ⇒ requestIp
      case false ⇒ xForwardedForIps.last
    }
  }

  def accept(mType: String) = acceptedMimeTypes.contains(mType)

  //Give Accepted mime types by priority, 
  //first is the most important one which is deduced from the extension, 
  //then come the accept header in priority order.
  lazy val acceptedMimeTypes: List[String] =
    (Filename.extension.unapply(path).
      map { ext ⇒ List(MimeType(ext)) }.
      getOrElse(List[String]()) ++
      acceptHeaders).distinct

  override def toString = method + " - " + path + "-" + headers + "-" + "\n" + body
  def toShortString = method + " - " + path

  protected def parseParams(params: String): Map[String, Seq[String]] = {
    ((params.split("&") map (_.split("=").toList)).foldLeft(Map[String, Seq[String]]()) {
      (params, param) ⇒
        (param match {
          case name :: value :: Nil ⇒ Some((decode(name), decode(value)))
          case name :: Nil          ⇒ Some((decode(name), ""))
          case _                    ⇒ None
        }) match {
          case Some((name, value)) if (params.contains(name)) ⇒
            params + (name -> (params(name) :+ value))
          case Some((name, value)) ⇒
            params + (name -> Seq(value))
          case None ⇒ params
        }

    })
  }

  protected def decode(s: String) = URLDecoder.decode(s, "UTF-8")
}

object Request {
  private val trustedAddress = """^127\.0\.0\.1$|^10\..+$|^192\.168\..+$|^172\.1[6-9]\..+$|^172\.2[0-9]\..+$|^172\.3[0-1]\..+$|^::1$|^fd[0-9a-f]{2}:.+|^localhost$|^unix$""".r
  def isTrusted(address: String) = {
    trustedAddress.pattern.matcher(address).matches
  }
}
