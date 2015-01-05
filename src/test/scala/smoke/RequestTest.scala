package smoke

import org.scalatest.FunSpecLike
import java.net.InetSocketAddress

class RequestTest extends FunSpecLike {
  val address = new InetSocketAddress("23.2.1.4", 80)

  describe("trusted addresses") {
    it("should match local addresses") {
      assert(Request.isTrusted("127.0.0.1") === true)
      assert(Request.isTrusted("localhost") === true)
      assert(Request.isTrusted("::1") === true)
    }

    it("should match unix sockets") {
      assert(Request.isTrusted("unix") === true)
    }

    it("should match reserved ip ranges") {
      assert(Request.isTrusted("10.0.0.1") === true)
      assert(Request.isTrusted("192.168.0.1") === true)
      assert(Request.isTrusted("172.16.0.1") === true)
      assert(Request.isTrusted("fd5b:982e:9130:247f:0000:0000:0000:0000") === true)
    }

    it("should extract empty params") {
      var req = new test.TestRequest("http://test.com?lorem=ipsum&empty=&test=test")
      assert(req.queryParams.get("empty") === Some(""))
      assert(req.formParams.get("empty") === None)
      assert(req.params("lorem") === "ipsum")
      assert(req.params("test") === "test")

      req = new test.TestRequest("http://test.com?empty=&lorem=ipsum")
      assert(req.queryParams.get("empty") === Some(""))
      assert(req.formParams.get("empty") === None)
      assert(req.params("lorem") === "ipsum")

      req = new test.TestRequest("http://test.com?empty=")
      assert(req.queryParams.get("empty") === Some(""))

      req = new test.TestRequest("http://test.com?=")
      assert(req.queryParams === Map())
    }
  }

  describe("acceptedMimeTypes") {
    it("should extract type based on the extension first") {
      val req = new test.TestRequest("http://test.com/test.xml")
      assertResult(List("application/xml"))(req.acceptedMimeTypes)
    }
    it("should extract type from the accept headers") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/json", "Accept" -> "application/xml"))
      assertResult(List("application/json", "application/xml"))(req.acceptedMimeTypes)
    }
    it("should extract type based on headers and extension, keeping extenstion the first in the list") {
      val req = new test.TestRequest("http://test.com/test.xml", headers = Seq("Accept" -> "application/json", "Accept" -> "application/xml"))
      assertResult(List("application/xml", "application/json"))(req.acceptedMimeTypes)
    }
    it("should sort accept headers by q parameters") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "text/plain;q=0.9", "Accept" -> "application/json", "Accept" -> "*/*;q=0.7", "Accept" -> "application/xml"))
      assertResult(List("application/json", "application/xml", "text/plain", "*/*"))(req.acceptedMimeTypes)
    }
    it("but also maintain the original order for the rest") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "text/plain;q=0.9", "Accept" -> "application/xml", "Accept" -> "application/json"))
      assertResult(List("application/xml", "application/json", "text/plain"))(req.acceptedMimeTypes)
    }
    it("and correctly handle malformed accept headers") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "text/plain;q=foo", "Accept" -> "application/xml;q=0.8", "Accept" -> "*/*;q=0.5"))
      assertResult(List("text/plain", "application/xml", "*/*"))(req.acceptedMimeTypes)
    }
  }

  describe("accept method") {
    it("should return true if the mimetype is in the list") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/xml", "Accept" -> "application/json"))
      assertResult(true)(req.accept("application/xml"))
    }
    it("should return true if the mimetype is not in the list") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/xml", "Accept" -> "application/json"))
      assertResult(false)(req.accept("text/plain"))
    }
  }

  describe("host") {
    it("should return the host stored in the headers") {
      val host = "test.host.com"
      val req = new test.TestRequest("/path/to/file", headers = Seq("Host" -> host))
      assert(req.host === host)
    }

    it("should exclude the port and return the host stored in the headers") {
      val host = "test.host.com"
      val req = new test.TestRequest("/path/to/file", headers = Seq("Host" -> s"$host:8080"))
      assert(req.host === host)
    }

    it("should return the host in URI if present") {
      val host = "test.host.com"
      val req = new test.TestRequest(s"http://$host/path/to/file")
      assert(req.host === host)
    }

    it("should return the host without port in URI if present") {
      val host = "test.host.com"
      val req = new test.TestRequest(s"http://$host:8080/path/to/file")
      assert(req.host === host)
    }

    it("should prefer the host in header if present in the URI as well") {
      val headerHost = "header.host.com"
      val uriHost = "uri.host.com"
      val req = new test.TestRequest(s"http://$uriHost:8080/path/to/file", headers = Seq("Host" -> s"$headerHost"))
      assert(req.host === headerHost)
    }

    it("should return null if neither the header nor the URI has the host") {
      val req = new test.TestRequest("/path/to/file")
      assert(req.host === null)
    }
  }

  describe("port") {

    it("should return None if host in header does not contain port") {
      val host = "test.host.com"
      val uri = "/path/to/file"
      val req = new test.TestRequest(uri)
      assert(req.port === None)
    }

    it("should exclude the host and return the port stored in the headers") {
      val port = 8080
      val host = "test.host.com"
      val uri = "/path/to/file"
      val req = new test.TestRequest(uri, headers = Seq("Host" -> s"$host:$port"))
      assert(req.port === Some(8080))
    }

    it("should return the port in uri if present") {
      val uriPort = 8080
      val host = "test.host.com"
      val uri = "/path/to/file"
      val req = new test.TestRequest(s"http://$host:$uriPort$uri")
      assert(req.port === Some(uriPort))
    }

    it("should return None if port not present in URI and header") {
      val port = 8080
      val host = "test.host.com"
      val uri = "/path/to/file"
      val req = new test.TestRequest(s"http://$host$uri")
      assert(req.port === None)
    }

    it("should prefer the port in Host Header if present in URI as well") {
      val uriPort = 9080
      val headerPort = 8080
      val host = "test.host.com"
      val uri = "/path/to/file"
      val req = new test.TestRequest(s"http://$host:$uriPort$uri", headers = Seq("Host" -> s"$host:$headerPort"))
      assert(req.port === Some(headerPort))
    }
  }

  describe("hostWithPort") {
    it("should return both host and port when port present") {
      val port = 6768
      val host = "test.host.com"
      val uri = "/path/to/file"
      val req = new test.TestRequest(s"http://$host:$port$uri")
      assert(req.hostWithPort === s"$host:$port")
    }

    it("should return both host without port when port not present") {
      val host = "test.host.com"
      val uri = "/path/to/file"
      val req = new test.TestRequest(s"http://$host$uri")
      assert(req.hostWithPort === host)
    }
  }

  describe("path") {
    it("should return request path") {
      val uri = "http://test.host/A134/B987?someVal=45.432&other+val=47.334"
      val req = new test.TestRequest(uri)
      assert(req.path === "/A134/B987")
    }
    it("should return / if there is no path") {
      val uri = "http://test.host/"
      val req = new test.TestRequest(uri)
      assert(req.path === "/")
    }
    it("should return '' if path is empty") {
      val uri = "http://test.host"
      val req = new test.TestRequest(uri)
      assert(req.path === "")
    }
  }

  describe("ip") {
    it("should return request address IP") {
      val req = new test.TestRequest("http://test.host", requestIp = "23.2.1.4")
      assert(req.ip === "23.2.1.4")
    }

    describe("proxy requests") {
      it("should prefer the x-forwarded-for over the request IP") {
        val req = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2.2.2.2"))
        assert(req.ip === "2.2.2.2")
      }

      it("should return the last x-forwarded-for IP when there is a list") {
        val req = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2.2.2.2, 3.3.3.3"))
        assert(req.ip === "3.3.3.3")
      }

      it("should filter out trusted addresses") {
        val request = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2.2.2.2, 127.0.0.1"))

        assert(request.ip === "2.2.2.2")

        val request2 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2.2.2.2, 10.0.0.1"))

        assert(request2.ip === "2.2.2.2")

        val request3 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2.2.2.2, 192.168.0.1"))

        assert(request3.ip === "2.2.2.2")

        val request4 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2.2.2.2, 172.16.0.1"))

        assert(request4.ip === "2.2.2.2")

        val request5 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "unknown, 192.168.0.1"))

        assert(request5.ip === "unknown")

        val request6 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "unknown, other, 192.168.0.1"))

        assert(request6.ip === "other")

        val request7 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "unknown, localhost, 192.168.0.1"))

        assert(request7.ip === "unknown")
      }

      it("should describe reserved IPs in the x-forwarded-for") {
        val request = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2.2.2.2, 3.3.3.3"))

        assert(request.ip === "3.3.3.3")
      }

      it("should handle reserved ipv6 addresses") {
        val request = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "::1,2620:0:1c00:0:812c:9583:754b:ca11"))

        assert(request.ip === "2620:0:1c00:0:812c:9583:754b:ca11")

        val request2 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2620:0:1c00:0:812c:9583:754b:ca11,::1"))

        assert(request2.ip === "2620:0:1c00:0:812c:9583:754b:ca11")

        val request3 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "fd5b:982e:9130:247f:0000:0000:0000:0000,2620:0:1c00:0:812c:9583:754b:ca11"))

        assert(request3.ip === "2620:0:1c00:0:812c:9583:754b:ca11")

        val request4 = new test.TestRequest("http://test.host",
          requestIp = "23.2.1.4",
          headers = Seq("X-Forwarded-For" -> "2620:0:1c00:0:812c:9583:754b:ca11,fd5b:982e:9130:247f:0000:0000:0000:0000"))

        assert(request4.ip === "2620:0:1c00:0:812c:9583:754b:ca11")
      }
    }
  }

  describe("queryString") {
    it("should return URI query string when present") {
      val uri = "http://test.host/A134/B987?someVal=45.432&other+val=47.334"
      val request = new test.TestRequest(uri)

      assert(request.queryString === Some("someVal=45.432&other+val=47.334"))
    }

    it("should return None when query string not present") {
      val uri = "http://test.host/A134/B987"
      val request = new test.TestRequest(uri)

      assert(request.queryString === None)
    }
  }

  describe("queryParams") {
    it("should return empty Map when no params included") {
      val request = new test.TestRequest("http://test.host")
      assert(request.queryParams === Map.empty)
    }

    it("should include decoded params when present") {
      val uri = "http://test.host/A134/B987?someVal=45.432&other+val=47.334"
      val request = new test.TestRequest(uri)

      assert(request.queryParams === Map(
        "someVal" -> "45.432",
        "other val" -> "47.334"))
    }
  }

  describe("contentType") {
    it("should return content type when present") {
      val uri = "/test"
      val requestA = new test.TestRequest(uri, headers = Seq("Content-Type" -> "text/html"))
      assert(requestA.contentType === Some("text/html"))

      val requestB = new test.TestRequest(uri, headers = Seq("Content-Type" -> "application/x-www-form-urlencoded"))
      assert(requestB.contentType === Some("application/x-www-form-urlencoded"))
    }

    it("should return None when content type not present") {
      val uri = "/test"
      val request = new test.TestRequest(uri)

      assert(request.contentType === None)
    }
  }

  describe("userAgent") {
    it("should return user agent when present") {
      val uri = "/test"
      val userAgentA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.57 Safari/534.24"
      val requestA = new test.TestRequest(uri, headers = Seq("User-Agent" -> userAgentA))
      assert(requestA.userAgent === Some(userAgentA))

      val userAgentB = "TestRequest/1.0.0"
      val requestB = new test.TestRequest(uri, headers = Seq("User-Agent" -> userAgentB))
      assert(requestB.userAgent === Some(userAgentB))
    }

    it("should return None when content type not present") {
      val uri = "/test"
      val requestA = new test.TestRequest(uri)
      assert(requestA.userAgent === None)
    }
  }

  describe("formParams") {
    it("should return empty Map when no params included") {
      val request = new test.TestRequest("/test")
      assert(request.formParams === Map.empty)
    }

    it("should return empty Map when Content-Type not set") {
      val request = new test.TestRequest("/test", body = "val=some+value")
      assert(request.formParams === Map.empty)
    }

    it("should return decoded params when present with Content-Type") {
      val request = new test.TestRequest("/test",
        body = "val=some+value",
        headers = Seq("Content-Type" -> "application/x-www-form-urlencoded"))
      assert(request.formParams === Map("val" -> "some value"))
    }

    it("should return decoded params when present with Content-Type and charset") {
      val request = new test.TestRequest("/test",
        body = "val=some+value",
        headers = Seq("Content-Type" -> "application/x-www-form-urlencoded; charset=UTF-8"))
      assert(request.formParams === Map("val" -> "some value"))
    }
  }

  describe("params") {
    it("should return both query and form params") {
      val uri = "http://test.host?query+val=some+value"
      val request = new test.TestRequest(uri,
        body = "form+val=other+value",
        headers = Seq("Content-Type" -> "application/x-www-form-urlencoded"))

      assert(request.params === Map(
        "query val" -> "some value",
        "form val" -> "other value"))
    }
  }

  describe("paramsValues") {
    it("should return both query and form params values") {
      val uri = "http://test.host?query+val=some+value&query+val=other+value"
      val request = new test.TestRequest(uri,
        body = "form+val=some+value&form+val=other+value",
        headers = Seq("Content-Type" -> "application/x-www-form-urlencoded"))

      assert(request.paramsValues === Map(
        "query val" -> Seq("some value", "other value"),
        "form val" -> Seq("some value", "other value")))
    }
  }
}
