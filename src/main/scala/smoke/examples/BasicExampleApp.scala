package smoke.examples

import smoke._
import com.typesafe.config.ConfigFactory


object BasicExampleApp extends Smoke with App {
  val smokeConfig = ConfigFactory.load().getConfig("smoke")
  def executionContext = scala.concurrent.ExecutionContext.global

  def onRequest = {
    case GET(Path("/example")) ⇒ reply {
      Thread.sleep(1000)
      Response(Ok, body = "It took me a second to build this response.\n")
    }
    case _ ⇒ reply(Response(NotFound))
  }

  override def after = {
    case response: Response ⇒
      val headers = response.headers ++ Map(
        "Server" -> "BasicExampleApp/0.0.1",
        "Connection" -> "Close")
      Response(response.status, headers, response.body)
  }

  start
}



object BasicExampleAppWithHandler extends App{
  new BasicExampleHandler start
}

class BasicExampleHandler extends Smoke {
  val smokeConfig = ConfigFactory.load().getConfig("smoke")
  def executionContext = scala.concurrent.ExecutionContext.global

  def onRequest = {
    case GET(Path("/example")) ⇒ reply {
      Thread.sleep(1000)
      Response(Ok, body = "It took me a second to build this response.\n")
    }
    case _ ⇒ reply(Response(NotFound))
  }

  override def after = {
    case response: Response ⇒
      val headers = response.headers ++ Map(
        "Server" -> "BasicExampleApp/0.0.1",
        "Connection" -> "Close")
      Response(response.status, headers, response.body)
  }
}

