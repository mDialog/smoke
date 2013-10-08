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

    it("should not return a file extension") {
      expectResult(None)(FileExtension.unapply("foo"))
    }
  }

  describe("Test FileName") {
    it("should extract a filename and extension") {
      expectResult(Some(List("foo", "m3u8")))(FileName.unapply("foo.m3u8"))
    }

    it("should extract filenames with multiple periods") {
      expectResult(Some(List("foo.bar.baz", "m3u8")))(FileName.unapply("foo.bar.baz.m3u8"))
    }

    it("should extract filenames with path segments") {
      expectResult(Some(List("/foo/bar/baz", "m3u8")))(FileName.unapply("/foo/bar/baz.m3u8"))
    }

    it("should extract a filename with no extension") {
      expectResult(Some(List("foo")))(FileName.unapply("foo"))
    }

    it("should extract a filename ending with a period but no extension") {
      expectResult(Some(List("foo")))(FileName.unapply("foo."))
    }

    it("should extract an empty filename an extension") {
      expectResult(Some(List("", "foo")))(FileName.unapply(".foo"))
    }

    it("should return None for an empty string") {
      expectResult(None)(FileName.unapply(""))
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
