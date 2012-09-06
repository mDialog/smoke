package smoke

trait ResponseData {
  def byteLength: Int
}
case class UTF8Data(data: String) extends ResponseData {
  def byteLength = data.getBytes.length

  override def toString = data
}
case class RawData(data: Array[Byte]) extends ResponseData {
  def byteLength = data.length
}

object ResponseData {
  implicit def str2UTF8Data(str: String) = UTF8Data(str)
}

case class Response(status: ResponseStatus,
  headers: Map[String, String] = Map.empty,
  body: ResponseData = "") {
  def statusCode = status.code
  def statusMessage = status.message

  def toMessage = messageStatus + messageHeaders + messageBody

  val contentLength = body.byteLength

  private def messageStatus = "HTTP/1.1 " + status.code + " " + status.message + "\r\n"
  private def messageHeaders = headers map { t ⇒ t._1 + ": " + t._2 } match {
    case Nil ⇒ ""
    case h ⇒ h.mkString("", "\r\n", "\r\n")
  }
  private def messageBody = body match {
    case utf8: UTF8Data ⇒ if (utf8.data.isEmpty) "" else "\r\n" + utf8.data
    //use a fixed length encoding for raw data
    case raw: RawData ⇒ new String(raw.data, "ISO-8859-1")
  }
}

case class ResponseStatus(code: Int, message: String)

object Continue extends ResponseStatus(100, "Continue")
object SwitchingProtocols extends ResponseStatus(101, "Switching Protocols")
object Processing extends ResponseStatus(102, "Processing")

object Ok extends ResponseStatus(200, "OK")
object Created extends ResponseStatus(201, "Created")
object Accepted extends ResponseStatus(202, "Accepted")
object NonAuthoritativeInformation extends ResponseStatus(203, "Non-Authoritative Information")
object NoContent extends ResponseStatus(204, "No Content")
object ResetContent extends ResponseStatus(205, "Reset Content")
object PartialContent extends ResponseStatus(206, "Partial Content")
object MultiStatus extends ResponseStatus(207, "Multi-Status")

object MultipleChoices extends ResponseStatus(300, "Multiple Choices")
object MovedPermanently extends ResponseStatus(301, "Moved Permanently")
object Found extends ResponseStatus(302, "Found")
object SeeOther extends ResponseStatus(303, "See Other")
object NotModified extends ResponseStatus(304, "Not Modified")
object UseProxy extends ResponseStatus(305, "Use Proxy")
object TemporaryRedirect extends ResponseStatus(307, "Temporary Redirect")

object BadRequest extends ResponseStatus(400, "Bad Request")
object Unauthorized extends ResponseStatus(401, "Unauthorized")
object PaymentRequired extends ResponseStatus(402, "Payment Required")
object Forbidden extends ResponseStatus(403, "Forbidden")
object NotFound extends ResponseStatus(404, "Not Found")
object MethodNotAllowed extends ResponseStatus(405, "No Content")
object NotAcceptable extends ResponseStatus(406, "Not Acceptable")
object ProxyAuthenticationRequired extends ResponseStatus(407, "Proxy Authentication Required")
object RequestTimeout extends ResponseStatus(408, "Request Timeout")
object Conflict extends ResponseStatus(409, "Conflict")
object Gone extends ResponseStatus(410, "Gone")
object LenghtRequired extends ResponseStatus(411, "Length Required")
object PreconditionFailed extends ResponseStatus(412, "Precondition Failed")
object RequestEntityTooLarge extends ResponseStatus(413, "Request Entity Too Large")
object RequestUriTooLong extends ResponseStatus(414, "Request-URI Too Long")
object UnsupportedMediaType extends ResponseStatus(415, "Unsupported Media Type")
object RequestRangeNotSatisfiable extends ResponseStatus(416, "Requested Range Not Satisfiable")
object ExpectationFailed extends ResponseStatus(417, "Expectation Failed")
object UnprocessableEntity extends ResponseStatus(422, "Unprocessable Entity")
object Locked extends ResponseStatus(423, "Locked")
object FailedDependency extends ResponseStatus(424, "Failed Dependency")

object InternalServerError extends ResponseStatus(500, "Internal Server Error")
object NotImplemented extends ResponseStatus(501, "Not Implemented")
object BadGateway extends ResponseStatus(502, "Bad Gateway")
object ServiceUnavailable extends ResponseStatus(503, "Service Unavailable")
object GatewayTimeout extends ResponseStatus(504, "Gateway Timeout")
object HTTPVersionNotSupported extends ResponseStatus(504, "HTTP Version Not Supported")
