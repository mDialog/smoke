package smoke.netty

import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.CookieDecoder
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URI
import collection.JavaConversions._
import smoke.Request

object NettyRequest {
  val cookieDecoder = new CookieDecoder()
  def extractCookies(request: HttpRequest): Map[String, String] =
    Option(request.headers.get(HttpHeaders.Names.COOKIE)).map {
      cookieDecoder.decode(_).map {
        c ⇒ (c.getName -> c.getValue)
      }.toMap
    }.getOrElse(Map())
}

case class NettyRequest(address: SocketAddress, nettyRequest: HttpRequest) extends Request {
  val version = nettyRequest.getProtocolVersion.toString
  val method = nettyRequest.getMethod.toString

  val uri = new URI(nettyRequest.getUri)
  val headers = nettyRequest.headers.entries map { e ⇒ (e.getKey.toLowerCase, e.getValue) } toSeq

  val keepAlive = HttpHeaders.isKeepAlive(nettyRequest)

  val requestIp = address.asInstanceOf[InetSocketAddress].getAddress.getHostAddress

  val cookies = NettyRequest.extractCookies(nettyRequest)

  val body = nettyRequest.getContent.toString(charset)
  val contentLength = nettyRequest.getContent.readableBytes
}
