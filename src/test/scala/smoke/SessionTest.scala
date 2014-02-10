package smoke

import org.scalatest.FunSpec

import smoke.test._

class SessionTest extends FunSpec {
  val appSecret = "0sfi034nrosd23kaldasl"
  val sessionManager = new SessionManager(appSecret)
  import sessionManager._

  describe("Create a session") {
    it("should generate all the cookies and sign them") {
      val cookies = Session(Map("user" -> "smoked", "timestamp" -> "1123121"))
      assert(cookies === Seq(
        "Set-Cookie" -> "user=smoked--49bc098016d8c0ef37606219f6b7102f4b0f2ac8",
        "Set-Cookie" -> "timestamp=1123121--d742c38c0bf77057750a9446d51548103ebface5"))
    }
  }

  describe("Extract a session from the cookies") {
    it("should return all the session values from the request") {
      val request = TestRequest("", headers = Seq(
        "Cookie" -> (s"user=smoked--" + sign("smoked")),
        "Cookie" -> (s"timestamp=0000--" + sign("0000"))))
      val Session(session) = request
      assert(session === Map("user" -> "smoked", "timestamp" -> "0000"))
    }
  }

  describe("Destroying a session should create empty cookies for all the values") {
    val cookies = Session.destroy(Map("user" -> "smoked", "timestamp" -> "1123121"))
    assert(cookies === Seq("Set-Cookie" -> "user=", "Set-Cookie" -> "timestamp="))
  }
}
