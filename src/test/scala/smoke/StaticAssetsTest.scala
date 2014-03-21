package smoke

import org.scalatest.FunSpecLike
import scala.concurrent.duration.Duration

import smoke.test._

class StaticAssetsTest extends FunSpecLike {

  class MockAsset extends StaticAssets {
    val publicFolder = "public"
  }
  val staticAssets = new MockAsset

  describe("when asset does not exist") {
    it("should respond with 404") {
      val response = staticAssets.responseFromAsset("/not-a-test-asset.html")
      assert(response === Response(NotFound))
    }
  }

  describe("when asset does exist") {
    it("should respond with asset") {
      import java.io.{ File, FileInputStream }
      val file = new File("src/test/resources/public/test-asset.html")
      val in = new FileInputStream(file)
      val bytes = new Array[Byte](file.length.toInt)
      in.read(bytes)
      in.close()

      val response = staticAssets.responseFromAsset("/test-asset.html")
      assert(response.status === Ok)
      assert(response.headers === Seq("Content-Type" -> "text/html"))
      assert(response.body.asInstanceOf[RawData].data === bytes)
    }
  }

}