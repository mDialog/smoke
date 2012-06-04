package smoke.examples

import smoke._

object FileApp extends Smoke {    
  onRequest {
    case GET(Path(path) & Path(FileExtension(extension)) ) => reply {
      //serve files in the resources folder
      val fullPath = "resources" + path

      try {
        import java.io.{File, FileInputStream}
        val file = new File(fullPath)
        val in = new FileInputStream(file)
        val bytes = new Array[Byte](file.length.toInt)
        in.read(bytes)
        in.close()
        
        Response(Ok, 
          body = RawData(bytes),
          headers = Map("Content-Type" -> (extension match {
            case "gif" => "image/gif"
            case "jpg" | "jpeg" => "image/jpeg"
            case _ => "application/octet-stream"
          })))
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


