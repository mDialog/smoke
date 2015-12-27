package smoke.netty

import org.scalatest.FunSpec

import java.net.InetSocketAddress
import java.net.URI

import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

class NettyRequestTest extends FunSpec {
  val localAddress = new InetSocketAddress("192.68.1.1", 80)
  val remoteAddress = new InetSocketAddress("22.2.1.4", 5432)

  describe("version") {
    it("should return HTTP 1.1 version") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.version === "HTTP/1.1")
    }

    it("should return HTTP 1.0 version") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_0, GET, "http://test.host")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.version === "HTTP/1.0")
    }
  }

  describe("method") {
    it("should return GET") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.method === "GET")
    }

    it("should return POST") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, POST, "http://test.host")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.method === "POST")
    }

    it("should return DELETE") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, DELETE, "http://test.host")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.method === "DELETE")
    }
  }

  describe("uri") {
    it("should return request URI") {
      val uri = "http://test.mdialog.com/some/path"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.uri === new URI(uri))
    }
  }

  describe("port") {
    it("should return the port when requested") {
      val uri = "http://test.mdialog.com/some/path"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.port === Some(localAddress.getPort))
    }
  }

  describe("headers") {
    it("should return request headers") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      HttpHeaders.setHeader(rawRequest, "Content-Type", "test/html")
      HttpHeaders.setHeader(rawRequest, "User-Agent", "TestRequest/1.0.0")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.headers === Seq(
        "content-type" -> "test/html",
        "user-agent" -> "TestRequest/1.0.0"))
    }
    it("should return request headers when charset is defined") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      HttpHeaders.setHeader(rawRequest, "Content-Type", "test/html;charset=UTF-8")
      HttpHeaders.setHeader(rawRequest, "User-Agent", "TestRequest/1.0.0")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.headers === Seq(
        "content-type" -> "test/html;charset=UTF-8",
        "user-agent" -> "TestRequest/1.0.0"))
    }
  }

  describe("keepAlive") {
    it("should return false if request is not keep alive") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, POST, "http://test.host")
      HttpHeaders.setKeepAlive(rawRequest, false)
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(!request.keepAlive)
    }

    it("should return true if request is keep alive") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, POST, "http://test.host")
      HttpHeaders.setKeepAlive(rawRequest, true)
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.keepAlive)
    }
  }

  describe("body") {
    it("should return request body when present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setContent(ChannelBuffers.copiedBuffer("greeting+dr=hello+goodbye", CharsetUtil.UTF_8));

      val request = NettyRequest(remoteAddress, localAddress, rawRequest)
      assert(request.body === "greeting+dr=hello+goodbye")
    }

    it("should return empty string when request body not present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")

      val request = NettyRequest(remoteAddress, localAddress, rawRequest)
      assert(request.body === "")
    }
  }

  describe("contentLength") {
    it("should return request body size when present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setContent(ChannelBuffers.copiedBuffer("test-test", CharsetUtil.UTF_8));

      val request = NettyRequest(remoteAddress, localAddress, rawRequest)
      assert(request.contentLength === 9)
    }

    it("should return 0 when request body not present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")

      val request = NettyRequest(remoteAddress, localAddress, rawRequest)
      assert(request.contentLength === 0)
    }
  }

  describe("cookies") {
    it("should return cookies") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      HttpHeaders.setHeader(rawRequest, "Cookie", "name=value; name2=value2")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.cookies == Map("name" -> "value", "name2" -> "value2"))
    }
    it("should return empty list when there are no cookies") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(remoteAddress, localAddress, rawRequest)

      assert(request.cookies == Map[String, String]())
    }
  }
}
