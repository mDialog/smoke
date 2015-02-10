package smoke

import org.scalatest.FunSpecLike
import scala.concurrent.duration.Duration

import smoke.test._
import java.io._

class StaticAssetsTest extends FunSpecLike {

  class MockAsset(cacheEnabled: Boolean) extends StaticAssets {
    val publicFolder = "public"
    override val cacheAssets = cacheEnabled
  }
  val staticAssets = new MockAsset(false)

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

    it("should be able to serve binary files") {
      import java.io.{ File, FileInputStream }
      val file = new File("src/test/resources/public/public-pixel.gif")
      val in = new FileInputStream(file)
      val bytes = new Array[Byte](file.length.toInt)
      in.read(bytes)
      in.close()

      val response = staticAssets.responseFromAsset("/public-pixel.gif")
      assert(response.status === Ok)
      assert(response.headers === Seq("Content-Type" -> "image/gif"))
      assert(response.body.asInstanceOf[RawData].data === bytes)
    }

    it("should not load anything outside of the static assets folder.") {
      assert(staticAssets.responseFromAsset("/../pixel.gif") === Response(NotFound))
      assert(staticAssets.responseFromAsset("/../plant.jpg") === Response(NotFound))
      assert(staticAssets.responseFromAsset("/../reference.conf") === Response(NotFound))
    }

    it("should not return anything if it's a folder") {
      staticAssets.responseFromAsset("/../") === Response(NotFound)
    }

    it("should load resources from jar too") {
      val response = staticAssets.responseFromAsset("/in-jar.html")
      assert(response.status === Ok)
    }

    it("should not cache the data when cache is disabled") {
      val staticAssets = new MockAsset(false)

      import scala.collection.JavaConversions._

      val prefix = this.getClass.getClassLoader.getResources("public").toList.map(_.getPath()).head
      val file = new File(s"$prefix/not-cached.txt")
      val pw = new PrintWriter(file)
      pw.write("before-cache")
      pw.close

      val response = staticAssets.responseFromAsset("/not-cached.txt")
      assert(response.status === Ok)
      assert(response.body.asInstanceOf[RawData].data === "before-cache".getBytes)

      val pw2 = new PrintWriter(file)
      pw2.write("after-cache")
      pw2.close

      val response2 = staticAssets.responseFromAsset("/not-cached.txt")
      assert(response2.status === Ok)
      assert(response2.body.asInstanceOf[RawData].data === "after-cache".getBytes)
    }

    it("should cache the data when cache is enabled") {
      val staticAssets = new MockAsset(true)

      import scala.collection.JavaConversions._

      val prefix = this.getClass.getClassLoader.getResources("public").toList.map(_.getPath()).head
      val file = new File(s"$prefix/cached.txt")
      val pw = new PrintWriter(file)
      pw.write("before-cache")
      pw.close

      val response = staticAssets.responseFromAsset("/cached.txt")
      assert(response.status === Ok)
      assert(response.body.asInstanceOf[RawData].data === "before-cache".getBytes)

      val pw2 = new PrintWriter(file)
      pw2.write("after-cache")
      pw2.close

      val response2 = staticAssets.responseFromAsset("/cached.txt")
      assert(response2.status === Ok)
      assert(response2.body.asInstanceOf[RawData].data === "before-cache".getBytes)
    }

    ignore("should not prevent from loading resources with sbt run") {}
    ignore("should not prevent from loading resources when smoke is bundled") {}
  }

}