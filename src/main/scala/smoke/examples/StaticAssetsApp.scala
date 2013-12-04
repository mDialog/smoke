package smoke.examples

import smoke._
import com.typesafe.config.ConfigFactory

object StaticAssetsSmoke extends SmokeApp with StaticAssets {
  val publicFolder = "src/test/resources/public"
  val config = ConfigFactory.load().getConfig("smoke")
  val executionContext = scala.concurrent.ExecutionContext.global

  onRequest {
    case GET(Path(path)) ⇒ reply(responseFromAsset(path))
    case _               ⇒ reply(Response(NotFound))
  }

  after { response ⇒
    val headers = response.headers ++ Seq(
      "Server" -> "StaticAssetsApp/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }
}

