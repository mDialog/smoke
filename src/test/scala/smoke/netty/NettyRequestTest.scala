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
    it("should return Some(port) when port present") {
      val uri = "http://test.host:6768/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri) 
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
    it("should return host without port when port present") {
      val uri = "http://test.host:6768/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri) 
      val request = NettyRequest(address, rawRequest)  
          
      assert(request.hostWithPort === "test.host:6768")
    }
    
    it("should return host without port when port not present") {
      val uri = "http://test.host/A134/B987"
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, uri) 
      val request = NettyRequest(address, rawRequest)  
          
      assert(request.hostWithPort === "test.host")
    }
  }
  
  describe("ip") {
    it("should return request address IP") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host") 
      val request = NettyRequest(new InetSocketAddress("23.2.1.4", 80), rawRequest)
     
      assert(request.ip === "23.2.1.4") 
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
        "other val" -> "47.334"
      ))
    }
  }
  
  describe("headers") {
    it("should return request headers") {
      val rawRequest = new DefaultHttpRequest(HTTP_1_1, GET, "http://test.host")
      rawRequest.setHeader("Content-Type", "test/html")
      rawRequest.setHeader("User-Agent", "TestRequest/1.0.0")
      val request = NettyRequest(address, rawRequest)
      
      assert(request.headers === Map(
        "Content-Type" -> "test/html",
        "User-Agent" -> "TestRequest/1.0.0"
      ))
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
        "form val" -> "other value"
      ))
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
}