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
}
