package smoke.mongrel2

import util.parsing.json.JSON
import java.net.URI
import smoke.Request

case class Mongrel2Request(rawData: Array[Byte]) extends Request {
  private val message = new String(rawData, "UTF-8")
  
  val (sender, connection, path, headers, body) = parse(message)    
  val method = headers("METHOD")
  val uri = new URI(headers("URI"))
  val hostWithPort = headers("host")
  val host = headers("host").split(":").head
  val port = headers("host").split(":").last.toInt
  val ip = headers("x-forwarded-for").split(",").head
  val keepAlive = false
  val queryString = headers.get("QUERY")

  val contentType = headers.get("content-type")
  val userAgent = headers.get("user-agent")
  
  val queryParams = parseParams(queryString.getOrElse(""))
  val formParams = contentType filter (_ == "application/x-www-form-urlencoded") match {
    case Some(t) => parseParams(body)
    case None => Map.empty[String, String]
  }
  val params = queryParams ++ formParams
      
  private def parse(message: String) = {
    val Array(sender, connection, path, rest) = message.split(" ", 4)   
    val (rawHeaders, bodyNetstring) = parseNetstring(rest)
    val headers = JSON.parseFull(rawHeaders).getOrElse(Map.empty).asInstanceOf[Map[String, String]]
    val (body, _) = parseNetstring(bodyNetstring)
    
    (sender, connection, path, headers, body)
  }
  
  private def parseNetstring(netstring: String) = {
    val Array(length, rest) = netstring.split(":", 2)
    (rest.substring(0, length.toInt), rest.substring(length.toInt + 1))
  }
}
