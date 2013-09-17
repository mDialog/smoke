package smoke

import org.scalatest.FunSpec
import akka.testkit.{ TestKit, ImplicitSender }
import akka.actor.ActorSystem
import scala.concurrent.duration.Duration

import smoke.test._

class StaticAssetsTest extends TestKit(ActorSystem("StaticAssetsTest"))
    with ImplicitSender with FunSpec {

  describe("when asset does not exist") {
    it("should respond with 404") {
      val actor = system.actorOf(StaticAssets("src/test/resources/public"))

      actor ! "/not-a-test-asset.html"

      expectMsg(Response(NotFound))
    }
  }

  describe("when asset does exist") {
    it("should respond with asset") {
      val actor = system.actorOf(StaticAssets("src/test/resources/public"))

      import java.io.{ File, FileInputStream }
      val file = new File("src/test/resources/public/test-asset.html")
      val in = new FileInputStream(file)
      val bytes = new Array[Byte](file.length.toInt)
      in.read(bytes)
      in.close()

      actor ! "/test-asset.html"

      val response = receiveOne(Duration("1s")).asInstanceOf[Response]
      assert(response.status === Ok)
      assert(response.headers === Seq("Content-Type" -> "text/html"))
      assert(response.body.asInstanceOf[RawData].data === bytes)
    }
  }

}