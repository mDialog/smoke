package smoke.test

import org.scalatest.FunSpec
import java.net.URI

class TestRequestTest extends FunSpec {

  describe("version") {
    it("should return HTTP 1.1 version") {
      val request = TestRequest("http://test.host:8080/some/path")

      assert(request.version === "HTTP/1.1")
    }
  }

  describe("method") {
    it("should be GET by default") {
      val request = TestRequest("http://test.host/some/path")
      assert(request.method === "GET")
    }

    it("should allow any value") {
      val request = TestRequest("http://test.host/some/path", "MKCOL")
      assert(request.method === "MKCOL")
    }
  }

  describe("uri") {
    it("should return URI object") {
      val request = TestRequest("http://test.host/some/path")
      assert(request.uri === new URI("http://test.host/some/path"))
    }
  }

  describe("path") {
    it("should return URI path") {
      val request = TestRequest("http://test.host/some/path")
      assert(request.path === "/some/path")
    }
  }

  describe("host") {
    it("should extract host from URI") {
      val request = TestRequest("http://test.host:8080/some/path")
      assert(request.host === "test.host")
    }
  }

  describe("port") {
    it("should extract port from URI") {
      val request = TestRequest("http://test.host:8080/some/path")
      assert(request.port === Some(8080))
    }

    it("should not extract port when not present") {
      val request = TestRequest("http://test.host/some/path")
      assert(request.port === None)
    }
  }

  describe("hostWithPort") {
    it("should compose from host and port when port is present") {
      val request = TestRequest("http://test.host:8080/some/path")
      assert(request.hostWithPort === "test.host:8080")
    }

    it("should compose from just host when port is not present") {
      val request = TestRequest("http://test.host/some/path")
      assert(request.hostWithPort === "test.host")
    }
  }

  describe("ip") {
    it("should return localhost IP") {
      val request = TestRequest("http://test.host:8080/some/path")
      assert(request.ip === "0.0.0.0")
    }
  }

  describe("keepAlive") {
    it("should return true by default") {
      val request = TestRequest("http://test.host:8080/some/path")
      assert(request.keepAlive)
    }

    it("can be set on initialization") {
      val request = TestRequest("http://test.host", keepAlive = false)
      assert(!request.keepAlive)
    }
  }

  describe("queryString") {
    it("should return None when no string present") {
      val request = TestRequest("http://test.host/path")
      assert(request.queryString === None)
    }

    it("should return raw query string when present") {
      val request = TestRequest("http://test.host/path?some=val&another=other+val")
      assert(request.queryString === Some("some=val&another=other+val"))
    }
  }

  describe("headers") {
    it("should return empty map when unset") {
      val request = TestRequest("http://test.host/path")
      assert(request.headers === Seq.empty)
    }

    it("should return allow headers to be set during initialization") {
      val request = TestRequest("http://test.host",
        headers = Seq(("content-type", "text/html; charset=UTF-8"),
          ("user-agent", "SmokeTest/1.0.0")))
      assert(request.headers === Seq(("content-type", "text/html; charset=UTF-8"),
        ("user-agent", "SmokeTest/1.0.0")))
    }

    describe("multiple headers") {
      val request = TestRequest("http://test.host",
        headers = Seq(("content-type", "text/html; charset=UTF-8"),
          ("x-foo", "Foo"),
          ("x-foo", "Bar"),
          ("user-agent", "SmokeTest/1.0.0")))
      it("should allow them") {
        assert(request.headers == Seq(("content-type", "text/html; charset=UTF-8"),
          ("x-foo", "Foo"),
          ("x-foo", "Bar"),
          ("user-agent", "SmokeTest/1.0.0")))
      }

      describe("fetching multiple headers") {
        it("should be able to fetch them all") {
          assert(request.allHeaderValues("x-foo") == Seq("Foo", "Bar"))
        }

        it("should be case-insensitive") {
          assert(request.allHeaderValues("X-Foo") == Seq("Foo", "Bar"))
        }
      }

      describe("fetching the last header value") {
        it("should be able to fetch the last one") {
          assert(request.lastHeaderValue("x-foo") == Some("Bar"))
        }

        it("should be case-insensitive") {
          assert(request.lastHeaderValue("X-Foo") == Some("Bar"))
        }
      }

      describe("collecting multiple header values") {
        it("should be able to make a concatenated String of all values for a given header") {
          assert(request.concatenateHeaderValues("x-foo") == Some("Foo,Bar"))
        }

        it("should be case-insensitive") {
          assert(request.concatenateHeaderValues("X-Foo") == Some("Foo,Bar"))
        }
      }
    }
  }

  describe("contentType") {
    it("should return None when header unset") {
      val request = TestRequest("http://test.host/path")
      assert(request.contentType === None)
    }

    it("should return Content-Type header when set") {
      val request = TestRequest("http://test.host",
        headers = Seq(("Content-Type", "text/html; charset=UTF-8")))
      assert(request.contentType === Some("text/html; charset=UTF-8"))
    }
  }

  describe("userAgent") {
    it("should return None when header unset") {
      val request = TestRequest("http://test.host/path")
      assert(request.userAgent === None)
    }

    it("should return User-Agent header when set") {
      val request = TestRequest("http://test.host",
        headers = Seq("User-Agent" -> "SmokeTest/1.0.0"))
      assert(request.userAgent === Some("SmokeTest/1.0.0"))
    }
  }

  describe("queryParams") {
    it("should return empty Map when no params included") {
      val request = TestRequest("http://test.host/path")
      assert(request.queryParams === Map.empty)
    }

    it("should include decoded params when present") {
      val request = TestRequest("http://test.host/path?val=some+value")
      assert(request.queryParams === Map("val" -> "some value"))
    }
  }

  describe("body") {
    it("should be empty string by default") {
      val request = TestRequest("http://test.host/path?val=some+value")
      assert(request.body == "")
    }

    it("should be set during intialization") {
      val request = TestRequest("http://test.host/path?val=some+value",
        body = "some body")
      assert(request.body == "some body")
    }
  }

  describe("contentLength") {
    it("should return request body size when present") {
      val request = TestRequest("http://test.host/path?val=some+value",
        body = "test-test")

      assert(request.contentLength === 9)
    }

    it("should return 0 when request body not present") {
      val request = TestRequest("http://test.host/path?val=some+value")

      assert(request.contentLength === 0)
    }
  }

  describe("formParams") {
    it("should return empty Map when no params included") {
      val request = TestRequest("http://test.host/path")
      assert(request.formParams === Map.empty)
    }

    it("should return empty Map when Content-Type not set") {
      val request = TestRequest("http://test.host/path?val=some+value",
        body = "val=some+value")
      assert(request.formParams === Map.empty)
    }

    it("should return decoded params when present with Content-Type") {
      val request = TestRequest("http://test.host/path?val=some+value",
        headers = Seq("Content-Type" -> "application/x-www-form-urlencoded"),
        body = "val=some+value")
      assert(request.formParams === Map("val" -> "some value"))
    }
  }

  describe("params") {
    it("should return both query and form params") {
      val request = TestRequest("http://test.host/path?query+val=some+value",
        headers = Seq("Content-Type" -> "application/x-www-form-urlencoded"),
        body = "form+val=other+value")
      assert(request.params === Map(
        "query val" -> "some value",
        "form val" -> "other value"))
    }
  }
}
