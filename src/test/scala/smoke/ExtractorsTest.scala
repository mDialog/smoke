package smoke

import org.scalatest.FunSpec

class ExtractorsTest extends FunSpec {
  describe("Test Seg") {
    it("should segment cleanly") {
      val list = Seg.unapply("/1/2/3/4/5/6/7")
      expectResult(7)(list.get.size)
    }

    it("should segment removing empty pieces") {
      val list = Seg.unapply("/1/2/3/4/5/6//7")
      expectResult(7)(list.get.size)
    }

    it("should segment removing many empty pieces") {
      val list = Seg.unapply("/1/2/3/4/5/6///7")
      expectResult(7)(list.get.size)
    }

    it("should segment removing empty pieces and multiple leading '/'") {
      val Seg(list) = "//1/2/3/4/5/6///7"
      expectResult(7)(list.size)
    }
  }

  describe("Test FileExtension") {
    it("should return a file extension") {
      expectResult(Some("m3u8"))(FileExtension.unapply("foo.m3u8"))
    }

    it("should extract extension with multiple periods") {
      expectResult(Some("m3u8"))(FileExtension.unapply("foo.bar.baz.m3u8"))
    }

    it("should extract extension in a path") {
      expectResult(Some("m3u8"))(FileExtension.unapply("/foo/path/foo.m3u8"))
    }

    it("should return none if the file does not have extension") {
      expectResult(None)(FileExtension.unapply("/foo/path/foo"))
    }

    it("should return none if the filename ends with a period") {
      expectResult(None)(FileExtension.unapply("/foo/path/foo."))
    }

    it("should not return a file extension") {
      expectResult(None)(FileExtension.unapply("foo"))
    }
  }

  describe("Filename") {

    it("should extract filenames with multiple periods") {
      expectResult(Some("foo.bar.baz.m3u8"))(Filename.unapply("/path/foo.bar.baz.m3u8"))
    }

    it("should extract filenames with path segments") {
      expectResult(Some("baz.m3u8"))(Filename.unapply("/foo/bar/baz.m3u8"))
    }

    it("should extract a filename with no extension") {
      expectResult(Some("foo"))(Filename.unapply("foo"))
    }

    it("should extract a filename ending with a period but no extension") {
      expectResult(Some("foo."))(Filename.unapply("foo."))
    }

    it("should return None for an empty string") {
      expectResult(None)(Filename.unapply(""))
    }
  }

  describe("ContentType") {
    it("should extract type based on the extension first") {
      val req = new test.TestRequest("http://test.com/test.xml")
      expectResult(Some("application/xml"))(ContentType.unapply(req))
    }
    it("should extract type based on the header if there is no extension") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/xml"))
      expectResult(Some("application/xml"))(ContentType.unapply(req))
    }
    it("should extract type based on the last header if there is no extension and multiple headers") {
      val req = new test.TestRequest("http://test.com/test", headers = Seq("Accept" -> "application/json", "Accept" -> "application/xml"))
      expectResult(Some("application/json"))(ContentType.unapply(req))
    }
  }

  describe("Test Method") {
    it("apply should return whether a request matches that method") {
      val req = new test.TestRequest("http://test.com")
      assert(GET(req))
      assert(!POST(req))
      assert(!PUT(req))
    }
  }
}
