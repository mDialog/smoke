package smoke.test

import org.scalatest.FunSpec
import java.net.URI

class TestRequestTest extends FunSpec {
  
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
    it("should return URI object")  {
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
      assert(request.port === 8080)
    }
  }
  
  describe("hostWithPort") {
    it("should compose form host and port") {
      val request = TestRequest("http://test.host:8080/some/path")
      assert(request.hostWithPort === "test.host:8080")
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
      assert(request.headers === Map.empty)
    }
    
    it("should return allow headers to be set during initialization") {
      val headers = Map(
        "Content-Type" -> "text/html; charset=UTF-8",
        "User-Agent" -> "SmokeTest/1.0.0"
      )
      val request = TestRequest("http://test.host",
                                headers = headers)
      assert(request.headers === headers)
    }
  }
  
  describe("contentType") {
    it("should return None when header unset") {
      val request = TestRequest("http://test.host/path")
      assert(request.contentType === None)
    }
    
    it("should return Content-Type header when set") {
      val request = TestRequest("http://test.host",
                                headers = Map("Content-Type" -> "text/html; charset=UTF-8"))
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
                                headers = Map("User-Agent" -> "SmokeTest/1.0.0"))
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
                                headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
                                body = "val=some+value")
      assert(request.formParams === Map("val" -> "some value"))
    }
  }
  
  describe("params") {
    it("should return both query and form params") {
      val request = TestRequest("http://test.host/path?query+val=some+value",
                                headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
                                body = "form+val=other+value")
      assert(request.params === Map(
        "query val" -> "some value",
        "form val" -> "other value"
      ))
    }
  }
  
}