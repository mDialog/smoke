package smoke.examples

import smoke._
import akka.actor._
import akka.pattern.ask

class SessionResponder extends Actor {
  val system = context.system

  def prettyHistory(history: String) =
    history.split(",")
      .mkString("Visit history:\n", "\n", "")

  def receive = {
    case r @ GET(Path("/get-access")) ⇒
      val cookies = Session(Map("has-access" -> "1"))
      sender ! Response(Ok, body = "<a href='/secured'>Access</a>", headers = cookies)

    case r @ GET(Path("/secured")) & Session(session) if session.get("has-access") == Some("1") ⇒
      val history = session.get("history").getOrElse("") + System.currentTimeMillis + ","
      val cookies = Session(session + ("history" -> history))
      sender ! Response(Ok, body = prettyHistory(history), headers = cookies)

    case r @ GET(Path("/secured")) ⇒
      sender ! Response(Unauthorized, body = "unauthorized")

    case r @ GET(Path("/remove-access")) & Session(session) ⇒
      sender ! Response(Ok, headers = Session.destroy(session))

    case _ ⇒
      sender ! Response(NotFound)
  }
}

object SessionExampleApp extends Smoke {

  val responder = system.actorOf(Props[SessionResponder], "responder")

  onRequest {
    case r: Request ⇒
      responder ? r mapTo manifest[Response]
  }

  after { response ⇒
    val headers = response.headers ++ Map(
      "Server" -> "SessionExampleApp/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }
}

