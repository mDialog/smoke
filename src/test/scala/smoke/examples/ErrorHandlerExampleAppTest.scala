package smoke.examples

import org.scalatest.{ FunSpec, BeforeAndAfterAll }

import akka.dispatch.Await
import akka.util.duration._

import smoke._
import smoke.test._

class ErrorHandlerExampleAppTest extends FunSpec with BeforeAndAfterAll {

  val app = ErrorHandlerExampleApp

  override def beforeAll { app.init() }
  override def afterAll { app.shutdown() }

  describe("GET /future-result-error") {
    it("should respond with 200") {
      val request = TestRequest("/future-result-error")
      val response = Await.result(app.application(request), 2 seconds)
      assert(response.body.toString === "Future result exception")
    }
  }

  describe("GET /request-handler-error") {
    it("should respond with 200") {
      val request = TestRequest("/request-handler-error")
      val response = Await.result(app.application(request), 2 seconds)
      assert(response.body.toString === "Request handler exception")
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
