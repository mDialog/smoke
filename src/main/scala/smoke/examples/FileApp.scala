package smoke.examples

import smoke._

object FileApp extends App with Smoke {    
  onRequest {
    case GET(Path("/test")) => reply {
      //Thread.sleep(1000)
      Response(Ok, body="It took me a second to build this response.\n")
    }
    case GET(Path(path) & Path(FileExtension(extension)) ) => reply {
      val fullPath = "resources" + path
      val contentType = extension match {
        case "gif" => "image/gif"
        case "jpg" | "jpeg" => "image/jpeg"
        case _ => null
      }

      try {
        val source = scala.io.Source.fromFile(fullPath)
        val byteArray = source.map(_.toByte).toArray
        source.close()
        Response(Ok, 
          body = RawData(byteArray),
          headers = contentType match {
            case null => Map()
            case other => Map("Content-Type" -> other)
          })
      } catch {
        case fnf: java.io.FileNotFoundException => Response(NotFound)
      }
    }
    case _ => reply(Response(NotFound))
  }
  
  after { response =>
    val headers = response.headers ++ Map(
      "Server" -> "FileApp/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }
}


