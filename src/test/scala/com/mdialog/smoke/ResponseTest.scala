package com.mdialog.smoke

import org.scalatest.FunSpec
import io.Source

class ResponseTest extends FunSpec {
  describe("statusCode") {
    it("should return status code") {
     assert(Response(Ok).statusCode == Ok.code) 
     assert(Response(NotFound).statusCode == NotFound.code) 
    }
  }
  
  describe("statusMessage") {
    it("should return status message") {
     assert(Response(Ok).statusMessage == Ok.message) 
     assert(Response(NotFound).statusMessage == NotFound.message) 
    }
  }
  
  describe("toMessage") {
    it("should serialize without headers or body") {
      val response = Response(Forbidden)

      val expected = "HTTP/1.1 403 Forbidden\r\n"
      assert(response.toMessage === expected)
    }
    
    it("should serialize with single header but without body") {
      val response = Response(Found, Map("Location" -> "http://mdialog.com/"))

      var expected = "HTTP/1.1 302 Found\r\nLocation: http://mdialog.com/\r\n"
      assert(response.toMessage === expected)
    }
    
    it("should serialize with multiple headers but without body") {
      val response = Response(Found, Map("Location" -> "http://mdialog.com/", "Accept" -> "*/*"))

      var expected = "HTTP/1.1 302 Found\r\nLocation: http://mdialog.com/\r\nAccept: */*\r\n"
      assert(response.toMessage === expected)
    }
    
    it("should serialize with body") {
      val headers = Map("Server" -> "scalatest", "Accept" -> "*/*")
      val body = "this is just a test"
      val response = Response(Ok, headers, body)

      var expected = "HTTP/1.1 200 OK\r\nServer: scalatest\r\nAccept: */*\r\n\r\nthis is just a test"
      assert(response.toMessage === expected)
    }
  }

}