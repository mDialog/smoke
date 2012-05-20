package smoke

import java.net.URLDecoder

trait Request {
  val method: String
  val uri: String
  val path: String
  val hostWithPort: String
  val host: String
  val port: Int
  val ip: String
  val keepAlive: Boolean
  val headers: Map[String, String]
  
  val queryString: Option[String]
  val contentType: Option[String]
  val userAgent: Option[String]
  
  val queryParams: Map[String, String]
  val formParams: Map[String, String]
  val params: Map[String, String]
  
  val body: String  
  
  override def toString = method + " - " + path + "-" + headers + "-" + "\n" + body

  protected def parseParams(params: String) =
    params.split("&") map (_.split("=")) map { p => 
      (decode(p.head), decode(p.last)) 
    } toMap
    
  protected def decode(s: String) = URLDecoder.decode(s, "UTF-8")
}

