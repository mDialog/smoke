package com.mdialog.smoke.netty

import org.scalatest.FunSpec

import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

class NettyRequestTest extends FunSpec {

  val uri = "http://test.mdialog.com:6768/video_assets/A134/streams/B987?latitude=45.432&longitude=47.334"
  val nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri) 
  nettyRequest.setContent(ChannelBuffers.copiedBuffer("greeting+dr=hello+goodbye", CharsetUtil.UTF_8));
  nettyRequest.setHeader("Content-Type", "application/x-www-form-urlencoded")
  nettyRequest.setHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.57 Safari/534.24")
  
  it("should parse from netty HttpRequest") {
    val address = new InetSocketAddress("23.2.1.4", 80)
    val request = NettyRequest(address, nettyRequest)
        
    assert(request.method === "POST")
    assert(request.uri === "http://test.mdialog.com:6768/video_assets/A134/streams/B987?latitude=45.432&longitude=47.334")
    assert(request.path === "/video_assets/A134/streams/B987")
    assert(request.hostWithPort === "test.mdialog.com:6768")
    assert(request.host === "test.mdialog.com")
    assert(request.port === 6768)
    assert(request.ip === "23.2.1.4")
    assert(request.queryString === Some("latitude=45.432&longitude=47.334"))
    
    assert(request.contentType === Some("application/x-www-form-urlencoded"))
    assert(request.userAgent === Some("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.57 Safari/534.24"))

    assert(request.body === "greeting+dr=hello+goodbye")

    assert(request.params.get("latitude") === Some("45.432"))
    assert(request.params.get("longitude") === Some("47.334"))
    assert(request.params.get("greeting dr") === Some("hello goodbye"))
  }
  
}