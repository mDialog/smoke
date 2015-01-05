package smoke

import org.scalatest.FunSpecLike

class ExtractorsTest extends FunSpecLike {
  describe("Test Seg") {
    it("should segment cleanly") {
      val list = Seg.unapply("/1/2/3/4/5/6/7")
      assertResult(7)(list.get.size)
    }

    it("should segment removing empty pieces") {
      val list = Seg.unapply("/1/2/3/4/5/6//7")
      assertResult(7)(list.get.size)
    }

    it("should segment removing many empty pieces") {
      val list = Seg.unapply("/1/2/3/4/5/6///7")
      assertResult(7)(list.get.size)
    }

    it("should segment removing empty pieces and multiple leading '/'") {
      val Seg(list) = "//1/2/3/4/5/6///7"
      assertResult(7)(list.size)
    }
  }

  describe("Params") {
    it("should extract the params with their first value") {
      assertResult(Some(Map("query val" -> "some value")))(Params.unapply(new test.TestRequest("http://test.host?query+val=some+value&query+val=other+value")))
    }
  }

  describe("ParamsValues") {
    it("should extract the params with their values") {
      assertResult(Some(Map("query val" -> List("some value", "other value"))))(ParamsValues.unapply(new test.TestRequest("http://test.host?query+val=some+value&query+val=other+value")))
    }
  }

  describe("Filename") {

    it("should extract filenames with multiple periods") {
      assertResult(Some("foo.bar.baz", "m3u8"))(Filename.unapply("/path/foo.bar.baz.m3u8"))
    }

    it("should extract filenames with path segments") {
      assertResult(Some("baz", "m3u8"))(Filename.unapply("/foo/bar/baz.m3u8"))
    }

    it("should extract a filename with no extension") {
      assertResult(Some("foo", ""))(Filename.unapply("foo"))
    }

    it("should extract a filename ending with a period but no extension") {
      assertResult(Some("foo", ""))(Filename.unapply("foo."))
    }

    it("should return None for an empty string") {
      assertResult(None)(Filename.unapply(""))
    }
  }

  describe("Test Filename.extension") {
    it("should return a file extension") {
      assertResult(Some("m3u8"))(Filename.extension.unapply("foo.m3u8"))
    }

    it("should extract extension with multiple periods") {
      assertResult(Some("m3u8"))(Filename.extension.unapply("foo.bar.baz.m3u8"))
    }

    it("should extract extension in a path") {
      assertResult(Some("m3u8"))(Filename.extension.unapply("/foo/path/foo.m3u8"))
    }

    it("should return none if the file does not have extension") {
      assertResult(None)(Filename.extension.unapply("/foo/path/foo"))
    }

    it("should return none if the filename ends with a period") {
      assertResult(None)(Filename.extension.unapply("/foo/path/foo."))
    }

    it("should not return a file extension") {
      assertResult(None)(Filename.extension.unapply("foo"))
    }
  }

  describe("Filename.base") {

    it("should extract basename with multiple periods") {
      assertResult(Some("foo.bar.baz"))(Filename.base.unapply("/path/foo.bar.baz.m3u8"))
    }

    it("should extract basename with path segments") {
      assertResult(Some("baz"))(Filename.base.unapply("/foo/bar/baz.m3u8"))
    }

    it("should extract a basename with no extension") {
      assertResult(Some("foo"))(Filename.base.unapply("foo"))
    }

    it("should extract a basename ending with a period but no extension") {
      assertResult(Some("foo"))(Filename.base.unapply("foo."))
    }

    it("should return None when it's an extension only") {
      assertResult(None)(Filename.base.unapply(".foo"))
    }

    it("should return None for an empty string") {
      assertResult(None)(Filename.base.unapply(""))
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
