package smoke.examples

import smoke._
import com.typesafe.config.ConfigFactory

object FileNameExampleApp extends SmokeApp {
  val smokeConfig = ConfigFactory.load().getConfig("smoke")

  val executionContext = scala.concurrent.ExecutionContext.global
  onRequest {
    case GET(Seg("products" :: productId :: Filename("orders", extension) :: Nil)) ⇒ reply {
      val responseText = extension match {
        case "xml"  ⇒ "<product id=\"${productId}\"><orders /></product>"
        case "json" ⇒ "{ product: { orders: [] } }"
        case _      ⇒ "plain text!"
      }
      Response(Ok, body = responseText)
    }

    case GET(Seg("products" :: productId :: "orders" :: Nil)) ⇒ reply {
      Response(Ok, body = "No extension")
    }

    // The previous case is just equivalent to:
    //case GET(Path(Seg("product" :: productId :: "orders" :: Nil))) ⇒ reply {
    //  Response(Ok, body = "No extension")
    //}

    case GET(Seg("products" :: productId :: "docs" :: _ :: Nil) & Filename.base(document)) ⇒ reply {

      document match {
        case "manual"   ⇒ Response(Ok, body = "1. Purchase our product. 2. ??? 3. Profit!")
        case "warranty" ⇒ Response(Ok, body = "Satisfaction guaranteed!")
        case _          ⇒ Response(NotFound)
      }
    }

    case _ ⇒ reply(Response(NotFound))
  }

  after { response ⇒
    val headers = response.headers ++ Map(
      "Server" -> "FileNameExampleApp/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }
}

