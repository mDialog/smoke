package smoke.examples

import org.scalatest.{ FunSpec, BeforeAndAfterAll }

import scala.concurrent.Await
import scala.concurrent.duration._

import smoke._
import smoke.test._

class FileServerAppTest extends FunSpec with BeforeAndAfterAll {

  val app = new FileServerSmoke()

  override def afterAll { app.shutdown() }

  describe("GET /plant.jpg") {
    it("should respond with 200") {
      val request = TestRequest("/plant.jpg")
      val response = Await.result(app.application(request), 2 seconds)
      assert(response.status === Ok)
    }

    it("should have content-type image/jpeg") {
      val request = TestRequest("/plant.jpg")
      val response = Await.result(app.application(request), 2 seconds)
      assert(response.lastHeaderValue("Content-Type") === Some("image/jpeg"))
    }

    it("body data should match the served file") {
      val request = TestRequest("/plant.jpg")
      val response = Await.result(app.application(request), 2 seconds)

      import java.io.{ File, FileInputStream }
      val file = new File("src/test/resources/plant.jpg")
      val in = new FileInputStream(file)
      val bytes = new Array[Byte](file.length.toInt)
      in.read(bytes)
      in.close()

      assert(response.body.asInstanceOf[RawData].data === bytes)
    }
  }

  describe("POST /unknown-path") {
    it("should respond with 404") {
      val request = TestRequest("/unknown-path", "POST")
      val response = Await.result(app.application(request), 2 seconds)
      assert(response.status === NotFound)
    }
  }
}
