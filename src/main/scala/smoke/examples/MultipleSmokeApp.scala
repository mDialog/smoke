package smoke.examples

import smoke._
import com.typesafe.config._
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

object MultipleSmokeApp extends App {
  val dispatcher = scala.concurrent.ExecutionContext.global

  val config = ConfigFactory.load("examples.conf")

  val smoke1 = new SampleSmoke(config.getConfig("smoke7772"), dispatcher)
  val smoke2 = new SampleSmoke(config.getConfig("smoke7773"), dispatcher)
}

class SampleSmoke(val smokeConfig: Config, val executionContext: ExecutionContext) extends Smoke {
  val ports = smokeConfig.getIntList("http.ports")

  onRequest {
    case GET(Path("/example")) ⇒ reply {
      Thread.sleep(1000)
      Response(Ok, body = "It took me a second to build this response.\n")
    }
    case _ ⇒ reply(Response(NotFound))
  }

  after { response ⇒
    val headers = response.headers ++ Map(
      "Server" -> s"MultipleSmokeApp-$ports/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }
}

