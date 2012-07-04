package smoke.examples

import org.scalatest.{ FunSpec, BeforeAndAfterAll }

import akka.dispatch.Await
import akka.util.duration._

import smoke._
import smoke.test._

class BasicExampleAppTest extends FunSpec with BeforeAndAfterAll {

  val app = BasicExampleApp

  override def beforeAll { app.init() }
  override def afterAll { app.shutdown() }

  describe("GET /example") {
    it("should respond with 200") {
      val request = TestRequest("/example")
      val response = Await.result(app.application(request), 2 seconds)
      assert(response.status === Ok)
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
