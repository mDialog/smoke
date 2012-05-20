package smoke.netty

import java.net.SocketAddress
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.util.CharsetUtil
import java.net.URI
import smoke.Request

case class NettyRequest(address: SocketAddress, nettyRequest: HttpRequest) 
  extends Request {
  private val u = new URI(nettyRequest.getUri)
    
  val method = nettyRequest.getMethod.toString
  val uri = nettyRequest.getUri
  val path = u.getPath
  val hostWithPort = u.getHost + ":" + u.getPort
  val host = u.getHost
  val port = u.getPort
  val ip = address.asInstanceOf[InetSocketAddress].getAddress.getHostAddress
  val keepAlive = HttpHeaders.isKeepAlive(nettyRequest)
  val headers = Map.empty[String, String]
  
  val queryString = if (u.getRawQuery == null) None else Some(u.getRawQuery)
  
  val contentType = 
    if (nettyRequest.getHeader("Content-Type") == null)
      None 
    else 
      Some(nettyRequest.getHeader("Content-Type"))
  
  val userAgent = 
    if (nettyRequest.getHeader("User-Agent") == null)
      None 
    else 
      Some(nettyRequest.getHeader("User-Agent"))
  
  val queryParams = 
    if (u.getRawQuery == null)
      Map.empty[String, String]
    else
      parseParams(u.getRawQuery)
  
  val formParams = contentType filter (_ == "application/x-www-form-urlencoded") match {
    case Some(t) => parseParams(nettyRequest.getContent.toString(CharsetUtil.UTF_8))
    case None => Map.empty[String, String]
  }
  val params = queryParams ++ formParams
  
  val body = nettyRequest.getContent.toString(CharsetUtil.UTF_8)
}