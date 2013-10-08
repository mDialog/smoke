package smoke.examples

import smoke._

object FileNameExampleApp extends Smoke {

  onRequest {
    case GET(Path(Seg("products" :: productId :: FileName("orders" :: extension :: Nil) :: Nil))) ⇒ reply {
      val responseText = extension match {
        case "xml"  ⇒ "<product id=\"${productId}\"><orders /></product>"
        case "json" ⇒ "{ product: { orders: [] } }"
        case _      ⇒ "plain text!"
      }
      Response(Ok, body = responseText)
    }

    case GET(Path(Seg("products" :: productId :: FileName("orders" :: Nil) :: Nil))) ⇒ reply {
      Response(Ok, body = "No extension")
    }

    // The previous case is just equivalent to:
    //case GET(Path(Seg("product" :: productId :: "orders" :: Nil))) ⇒ reply {
    //  Response(Ok, body = "No extension")
    //}

    case GET(Path(Seg("products" :: productId :: "docs" :: FileName(document :: "txt" :: Nil) :: Nil))) ⇒ reply {

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

