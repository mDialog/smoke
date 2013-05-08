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
  val address = new InetSocketAddress("23.2.1.4", 80)

  describe("version") {
    it("should return HTTP 1.1 version") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.version === "HTTP/1.1")
    }

    it("should return HTTP 1.0 version") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_0, GET, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.version === "HTTP/1.0")
    }
  }

  describe("method") {
    it("should return GET") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.method === "GET")
    }

    it("should return POST") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, POST, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.method === "POST")
    }

    it("should return DELETE") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, DELETE, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.method === "DELETE")
    }
  }

  describe("uri") {
    it("should return request URI") {
      val uri = "http://test.mdialog.com/some/path"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.uri === new URI(uri))
    }
  }

  describe("path") {
    it("should return request path") {
      val uri = "http://test.host/A134/B987?someVal=45.432&other+val=47.334"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.path === "/A134/B987")
    }
  }

  describe("port") {
    it("should return Some(port) when port present in uri") {
      val uri = "http://test.host:6768/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.port === Some(6768))
    }

    it("should return Some(port) when port present in header") {
      val uri = "/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", "test.host:6768")
      val request = NettyRequest(address, rawRequest)

      assert(request.port === Some(6768))
    }

    it("should return Some(port) from uri when port present in uri and header") {
      val uri = "http://test.host:6768/A134/B987"
      val header = "test.host:6769"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", header)
      val request = NettyRequest(address, rawRequest)

      assert(request.port === Some(6768))
    }

    it("should return None when port not present") {
      val uri = "http://test.host/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.port === None)
    }
  }

  describe("hostWithPort") {
    it("should return both host and port when port present") {
      val uri = "http://test.host:6768/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.hostWithPort === "test.host:6768")
    }

    it("should return both host without port when port not present") {
      val uri = "http://test.host/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.hostWithPort === "test.host")
    }
  }

  describe("host") {
    it("should return host when present in URI") {
      val uri = "http://test.host/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.host === "test.host")
    }

    it("should return host without port if present in URI") {
      val uri = "http://test.host:9090/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.host === "test.host")
    }

    it("should return host if present in header") {
      val uri = "/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", "test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.host === "test.host")
    }

    it("should return host without port when present in header") {
      val uri = "/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", "test.host:9090")
      val request = NettyRequest(address, rawRequest)

      assert(request.host === "test.host")
    }

    it("should return null if host not in URI or header") {
      val uri = "/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.host === null)
    }
  }

  describe("ip") {
    it("should return request address IP") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)

      assert(request.ip === "23.2.1.4")
    }

    describe("proxy requests") {
      it("should return the request IP by default") {
        val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)

        assert(request.ip === "23.2.1.4")
      }

      it("should prefer the x-forwarded-for over the request IP") {
        val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest.setHeader("X-Forwarded-For", "2.2.2.2")
        val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)

        assert(request.ip === "2.2.2.2")
      }

      it("should return the last x-forwarded-for IP when there is a list") {
        val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest.setHeader("X-Forwarded-For", "2.2.2.2, 3.3.3.3")
        val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)

        assert(request.ip === "3.3.3.3")
      }

      it("should filter out trusted addresses") {
        val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest.setHeader("X-Forwarded-For", "2.2.2.2, 127.0.0.1")
        val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)

        assert(request.ip === "2.2.2.2")

        val rawRequest2 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest2.setHeader("X-Forwarded-For", "2.2.2.2, 10.0.0.1")
        val request2 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest2)

        assert(request2.ip === "2.2.2.2")

        val rawRequest3 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest3.setHeader("X-Forwarded-For", "2.2.2.2, 192.168.0.1")
        val request3 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest3)

        assert(request3.ip === "2.2.2.2")

        val rawRequest4 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest4.setHeader("X-Forwarded-For", "2.2.2.2, 172.16.0.1")
        val request4 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest4)

        assert(request4.ip === "2.2.2.2")

        val rawRequest5 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest5.setHeader("X-Forwarded-For", "unknown, 192.168.0.1")
        val request5 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest5)

        assert(request5.ip === "unknown")

        val rawRequest6 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest6.setHeader("X-Forwarded-For", "unknown, other, 192.168.0.1")
        val request6 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest6)

        assert(request6.ip === "other")

        val rawRequest7 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest7.setHeader("X-Forwarded-For", "unknown, localhost, 192.168.0.1")
        val request7 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest7)

        assert(request7.ip === "unknown")
      }

      it("should describe reserved IPs in the x-forwarded-for") {
        val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest.setHeader("X-Forwarded-For", "2.2.2.2, 3.3.3.3")
        val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)

        assert(request.ip === "3.3.3.3")
      }

      it("should handle reserved ipv6 addresses") {
        val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest.setHeader("X-Forwarded-For", "::1,2620:0:1c00:0:812c:9583:754b:ca11")
        val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)

        assert(request.ip === "2620:0:1c00:0:812c:9583:754b:ca11")

        val rawRequest2 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest2.setHeader("X-Forwarded-For", "2620:0:1c00:0:812c:9583:754b:ca11,::1")
        val request2 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest2)

        assert(request2.ip === "2620:0:1c00:0:812c:9583:754b:ca11")

        val rawRequest3 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest3.setHeader("X-Forwarded-For", "fd5b:982e:9130:247f:0000:0000:0000:0000,2620:0:1c00:0:812c:9583:754b:ca11")
        val request3 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest3)

        assert(request3.ip === "2620:0:1c00:0:812c:9583:754b:ca11")

        val rawRequest4 = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
        rawRequest4.setHeader("X-Forwarded-For", "2620:0:1c00:0:812c:9583:754b:ca11,fd5b:982e:9130:247f:0000:0000:0000:0000")
        val request4 = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest4)

        assert(request4.ip === "2620:0:1c00:0:812c:9583:754b:ca11")
      }
    }
  }

  describe("queryString") {
    it("should return URI query string when present") {
      val uri = "http://test.host/A134/B987?someVal=45.432&other+val=47.334"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.queryString === Some("someVal=45.432&other+val=47.334"))
    }

    it("should return None when query string not present") {
      val uri = "http://test.host/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.queryString === None)
    }
  }

  describe("queryParams") {
    it("should return empty Map when no params included") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(address, rawRequest)
      assert(request.queryParams === Map.empty)
    }

    it("should include decoded params when present") {
      val uri = "http://test.host/A134/B987?someVal=45.432&other+val=47.334"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      val request = NettyRequest(address, rawRequest)

      assert(request.queryParams === Map(
        "someVal" -> "45.432",
        "other val" -> "47.334"))
    }
  }

  describe("headers") {
    it("should return request headers") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setHeader("Content-Type", "test/html")
      rawRequest.setHeader("User-Agent", "TestRequest/1.0.0")
      val request = NettyRequest(address, rawRequest)

      assert(request.headers === Seq(
        "content-type" -> "test/html",
        "user-agent" -> "TestRequest/1.0.0"))
    }
  }

  describe("contentType") {
    it("should return content type when present") {
      val rawRequestA = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequestA.setHeader("Content-Type", "text/html")
      val requestA = NettyRequest(address, rawRequestA)
      assert(requestA.contentType === Some("text/html"))

      val rawRequestB = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequestB.setHeader("content-type", "application/x-www-form-urlencoded")
      val requestB = NettyRequest(address, rawRequestB)
      assert(requestB.contentType === Some("application/x-www-form-urlencoded"))
    }

    it("should return None when content type not present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.contentType === None)
    }
  }

  describe("userAgent") {
    it("should return user agent when present") {
      val rawRequestA = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequestA.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.57 Safari/534.24")
      val requestA = NettyRequest(address, rawRequestA)
      assert(requestA.userAgent === Some("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.57 Safari/534.24"))

      val rawRequestB = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequestB.setHeader("user-agent", "TestRequest/1.0.0")
      val requestB = NettyRequest(address, rawRequestB)
      assert(requestB.userAgent === Some("TestRequest/1.0.0"))
    }

    it("should return None when content type not present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.userAgent === None)
    }
  }

  describe("body") {
    it("should return request body when present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setContent(ChannelBuffers.copiedBuffer("greeting+dr=hello+goodbye", CharsetUtil.UTF_8));

      val request = NettyRequest(address, rawRequest)
      assert(request.body === "greeting+dr=hello+goodbye")
    }

    it("should return empty string when request body not present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")

      val request = NettyRequest(address, rawRequest)
      assert(request.body === "")
    }
  }

  describe("contentLength") {
    it("should return request body size when present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setContent(ChannelBuffers.copiedBuffer("test-test", CharsetUtil.UTF_8));

      val request = NettyRequest(address, rawRequest)
      assert(request.contentLength === 9)
    }

    it("should return 0 when request body not present") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")

      val request = NettyRequest(address, rawRequest)
      assert(request.contentLength === 0)
    }
  }

  describe("formParams") {
    it("should return empty Map when no params included") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      val request = NettyRequest(address, rawRequest)

      assert(request.formParams === Map.empty)
    }

    it("should return empty Map when Content-Type not set") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setContent(ChannelBuffers.copiedBuffer("val=some+value", CharsetUtil.UTF_8))
      val request = NettyRequest(address, rawRequest)

      assert(request.formParams === Map.empty)
    }

    it("should return decoded params when present with Content-Type") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setHeader("Content-Type", "application/x-www-form-urlencoded")
      rawRequest.setContent(ChannelBuffers.copiedBuffer("val=some+value", CharsetUtil.UTF_8))
      val request = NettyRequest(address, rawRequest)

      assert(request.formParams === Map("val" -> "some value"))
    }
  }

  describe("params") {
    it("should return both query and form params") {
      val uri = "http://test.host?query+val=some+value"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.setHeader("Content-Type", "application/x-www-form-urlencoded")
      rawRequest.setContent(ChannelBuffers.copiedBuffer("form+val=other+value", CharsetUtil.UTF_8))
      val request = NettyRequest(address, rawRequest)

      assert(request.params === Map(
        "query val" -> "some value",
        "form val" -> "other value"))
    }
  }

  describe("keepAlive") {
    it("should return false if request is not keep alive") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, POST, "http://test.host")
      HttpHeaders.setKeepAlive(rawRequest, false)
      val request = NettyRequest(address, rawRequest)

      assert(!request.keepAlive)
    }

    it("should return true if request is keep alive") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, POST, "http://test.host")
      HttpHeaders.setKeepAlive(rawRequest, true)
      val request = NettyRequest(address, rawRequest)

      assert(request.keepAlive)
    }
  }

  describe("extractHost") {

    it("should return the host stored in the headers") {
      val host = "test.host.com"
      val uri = "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", host)

      assert(NettyRequest.extractHost(rawRequest) === host)
    }

    it("should exclude the port and return the host stored in the headers") {
      val host = "test.host.com"
      val uri = "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", host + ":8080")

      assert(NettyRequest.extractHost(rawRequest) === host)
    }

    it("should return the host in URI if present") {
      val host = "test.host.com"
      val uri = "http://" + host + "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)

      assert(NettyRequest.extractHost(rawRequest) === host)
    }

    it("should return the host without port in URI if present") {
      val host = "test.host.com"
      val uri = "http://" + host + ":8080/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)

      assert(NettyRequest.extractHost(rawRequest) === host)
    }

    it("should prefer the host in header if present in the URI as well") {
      val headerHost = "test.host.com"
      val uriHost = "uri.host.com"
      val uri = "http://" + uriHost + "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", headerHost)

      assert(NettyRequest.extractHost(rawRequest) === headerHost)
    }

    it("should return null if neither the header nor the URI has the host") {
      val uri = "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)

      assert(NettyRequest.extractHost(rawRequest) === null)
    }
  }

  describe("extractPort") {

    it("should return None if host in header does not contain port") {
      val host = "test.host.com"
      val uri = "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", host)

      assert(NettyRequest.extractPort(rawRequest) === None)
    }

    it("should exclude the host and return the port stored in the headers") {
      val port = 8080
      val uri = "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", "test.mdialog.com:" + port.toString)

      assert(NettyRequest.extractPort(rawRequest).get === port)
    }

    it("should return the port in URI if present") {
      val port = 9080
      val uri = "http://test.mdialog.com:" + port.toString + "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)

      assert(NettyRequest.extractPort(rawRequest).get === port)
    }

    it("should return None if port not present in URI") {
      val uri = "http://test.host.com/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)

      assert(NettyRequest.extractPort(rawRequest) === None)
    }

    it("should prefer the port in URI if present in header as well") {
      val headerPort = 9090
      val uriPort = 8080
      val uri = "http://test.mdialog.com:" + uriPort + "/path/to/file"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri)
      rawRequest.addHeader("host", "test.mdialog.com:" + headerPort)

      assert(NettyRequest.extractPort(rawRequest).get === uriPort)
    }
  }
}
