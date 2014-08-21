package smoke.examples

import org.scalatest.{ FunSpec }

import scala.concurrent.Await
import scala.concurrent.duration._

import smoke._
import smoke.test._

class BasicExampleSmokeTest extends FunSpec {

  val app = new BasicExampleSmoke with TestSmoke

  implicit val timeout = Duration(2, SECONDS)

  describe("GET /example") {
    it("should respond with 200") {
      val request = TestRequest("/example")
      val response = app.sendAwait(request)
      assert(response.status === Ok)
    }
  }

  describe("POST /unknown-path") {
    it("should respond with 404") {
      val request = TestRequest("/unknown-path", "POST")
      val response = app.sendAwait(request)
      assert(response.status === NotFound)
    }
  }
}

class BasicExampleAppTest extends FunSpec {

  val app = TestSmokeApp(BasicExampleApp)

  implicit val timeout = Duration(2, SECONDS)

  describe("GET /example") {
    it("should respond with 200") {
      val request = TestRequest("/example")
      val response = app.sendAwait(request)
      assert(response.status === Ok)
    }
  }

  describe("POST /unknown-path") {
    it("should respond with 404") {
      val request = TestRequest("/unknown-path", "POST")
      val response = app.sendAwait(request)
      assert(response.status === NotFound)
    }
  }
}
