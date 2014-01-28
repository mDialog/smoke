package smoke

import org.scalatest.FunSpec

class RequestTest extends FunSpec {
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
      expectResult(List("application/xml"))(req.acceptedMimeTypes)
    }
    it("should extract type from the accept headers") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/json", "Accept" -> "application/xml"))
      expectResult(List("application/json", "application/xml"))(req.acceptedMimeTypes)
    }
    it("should extract type based on headers and extension, keeping extenstion the first in the list") {
      val req = new test.TestRequest("http://test.com/test.xml", headers = Seq("Accept" -> "application/json", "Accept" -> "application/xml"))
      expectResult(List("application/xml", "application/json"))(req.acceptedMimeTypes)
    }
    it("should sort accept headers by q parameters") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "text/plain;q=0.9", "Accept" -> "application/json", "Accept" -> "*/*;q=0.7", "Accept" -> "application/xml"))
      expectResult(List("application/json", "application/xml", "text/plain", "*/*"))(req.acceptedMimeTypes)
    }
    it("but also maintain the original order for the rest") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "text/plain;q=0.9", "Accept" -> "application/xml", "Accept" -> "application/json"))
      expectResult(List("application/xml", "application/json", "text/plain"))(req.acceptedMimeTypes)
    }
  }

  describe("accept method") {
    it("should return true if the mimetype is in the list") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/xml", "Accept" -> "application/json"))
      expectResult(true)(req.accept("application/xml"))
    }
    it("should return true if the mimetype is not in the list") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/xml", "Accept" -> "application/json"))
      expectResult(false)(req.accept("text/plain"))
    }
  }
}
