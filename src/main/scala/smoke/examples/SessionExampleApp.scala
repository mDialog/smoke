package smoke.examples

import smoke._
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext

object SessionExampleApp extends SmokeApp {
  val executionContext = scala.concurrent.ExecutionContext.global
  val smokeConfig = ConfigFactory.load().getConfig("smoke")

  val sessionManager = new SessionManager(smokeConfig.getString("session.secret"))
  import sessionManager._

  def prettyHistory(history: String) =
    history.split(",")
      .mkString("Visit history:\n", "\n", "")

  onRequest {
    case r @ GET(Path("/get-access")) ⇒
      val cookies = Session(Map("has-access" -> "1"))
      reply(Response(Ok, body = "<a href='/secured'>Access</a>", headers = cookies))

    case r @ GET(Path("/secured")) & Session(session) if session.get("has-access") == Some("1") ⇒
      val history = session.get("history").getOrElse("") + System.currentTimeMillis + ","
      val cookies = Session(session + ("history" -> history))
      reply(Response(Ok, body = prettyHistory(history), headers = cookies))

    case r @ GET(Path("/secured")) ⇒
      reply(Response(Unauthorized, body = "unauthorized"))

    case r @ GET(Path("/remove-access")) & Session(session) ⇒
      reply(Response(Ok, headers = Session.destroy(session)))

    case _ ⇒
      reply(Response(NotFound))
  }

  after { response ⇒
    val headers = response.headers ++ Map(
      "Server" -> "SessionExampleApp/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }
}

