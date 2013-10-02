package smoke.netty

import java.net.SocketAddress
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.util.CharsetUtil
import java.nio.charset.Charset
import java.net.URI
import collection.JavaConversions._
import scala.util.Try
import smoke.Request

object NettyRequest {
  def extractHost(request: HttpRequest) = {
    val u = new URI(request.getUri)
    if (request.containsHeader("host")) request.getHeader("host").split(":").head
    else if (!Option(u.getHost).isEmpty) u.getHost
    else null
  }

  def extractPort(request: HttpRequest): Option[Int] = {
    val u = new URI(request.getUri)
    if (u.getPort >= 0) Some(u.getPort)
    else if (request.containsHeader("host")) {
      val result = request.getHeader("host").split(":")
      if (result.length > 1) Some(result(1).toInt)
      else None
    } else None
  }

  val cookieDecoder = new CookieDecoder()
  def extractCookies(request: HttpRequest): Map[String, String] =
    Option(request.getHeader(HttpHeaders.Names.COOKIE)).map {
      cookieDecoder.decode(_).map {
        c ⇒ (c.getName -> c.getValue)
      }.toMap
    }.getOrElse(Map())

}

case class NettyRequest(address: SocketAddress, nettyRequest: HttpRequest)
    extends Request {
  private val u = new URI(nettyRequest.getUri)

  val version = nettyRequest.getProtocolVersion.toString
  val method = nettyRequest.getMethod.toString
  val uri = new URI(nettyRequest.getUri)
  val path = u.getPath
  val host = NettyRequest.extractHost(nettyRequest)
  val port = NettyRequest.extractPort(nettyRequest)
  val hostWithPort = host + (port map (":" + _.toString) getOrElse (""))
  val headers = nettyRequest.getHeaders map { e ⇒ (e.getKey.toLowerCase, e.getValue) } toSeq

  val ip = {
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
      case true  ⇒ address.asInstanceOf[InetSocketAddress].getAddress.getHostAddress
      case false ⇒ xForwardedForIps.last
    }
  }

  val keepAlive = HttpHeaders.isKeepAlive(nettyRequest)

  val queryString = if (u.getRawQuery == null) None else Some(u.getRawQuery)

  val contentType =
    if (nettyRequest.getHeader("Content-Type") == null)
      None
    else
      Some(nettyRequest.getHeader("Content-Type"))

  val charset = contentType match {
    case Some(t) ⇒
      t.split(";").filter(_.contains("charset")).map(_.split("=")).map(x ⇒ (x.last)).toSeq.headOption match {
        case Some(c) ⇒ Try(Charset.forName(c)).toOption.getOrElse(CharsetUtil.UTF_8)
        case None    ⇒ CharsetUtil.UTF_8
      }
    case None ⇒ CharsetUtil.UTF_8
  }

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

  val formParams = contentType filter (_.toLowerCase.startsWith("application/x-www-form-urlencoded")) match {
    case Some(t) ⇒
      parseParams(nettyRequest.getContent.toString(charset))
    case None ⇒ Map.empty[String, String]
  }
  val params = queryParams ++ formParams

  val cookies = NettyRequest.extractCookies(nettyRequest)

  val body = nettyRequest.getContent.toString(charset)
  val contentLength = nettyRequest.getContent.readableBytes

}
