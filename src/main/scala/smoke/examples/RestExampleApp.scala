package smoke.examples

import smoke._
import scala.concurrent._
import com.typesafe.config.ConfigFactory

/*
This example uses StaticAssets, Session, Accept and ContentType.
*/
object RestExampleApp extends App {
  val smoke = new RestExampleSmoke
}

//Authentication extractor
object Authenticated {
  private val apiKey = "key"
  val sessionMngr = new SessionManager("secret")
  import sessionMngr._

  def unapply(request: Request): Option[Boolean] =
    Some(Session.unapply(request).map { session ⇒
      session.get("authenticated") == Some("yes")
    } orElse request.params.get("api_key").map {
      x ⇒ x == apiKey
    } getOrElse (false))
}

class RestExampleSmoke extends Smoke with StaticAssets {
  val smokeConfig = ConfigFactory.load().getConfig("smoke")
  implicit val executionContext = scala.concurrent.ExecutionContext.global
  import Authenticated.sessionMngr._

  val publicFolder = "."

  type Responder = PartialFunction[Request, Future[Response]]

  // public :  /login
  // public :  /... (static assets)
  // private:  /book

  val publicResponder: Responder = {
    case r @ POST(Path("/login")) if r.accept("text/html") ⇒
      val cookies = Session(Map("authenticated" -> "yes"))
      reply(Response(Found, headers = Seq("Location" -> s"book") ++ cookies))
    case r @ GET(Path("/")) ⇒
      reply(Response(Found, headers = Seq("Location" -> s"book")))
    case GET(Path(path)) ⇒
      reply(responseFromAsset(path))
    case _ ⇒
      reply(Response(NotFound))
  }

  val privateResponder: Responder = {
    case r @ GET(Seg(Filename.base("book") :: Nil)) ⇒
      reply(Response(Ok, headers = Seq("Content-Type" -> "text/html"), body = "<html>Books !</html>")) //UI only

    case r @ POST(Seg(Filename.base("book") :: Nil)) & ContentType("application/xml") ⇒
      //Create a book here
      val id = "new-id"
      reply(Response(Created, headers = Seq("Location" -> s"books/$id")))

    case POST((Seg(Filename.base("book") :: Nil))) ⇒ //Unsupported content type
      reply(Response(BadRequest))

    case r @ GET(Seg("book" :: Filename(id, extension) :: Nil)) ⇒
      reply(r.acceptedMimeTypes.collectFirst {
        case "application/xml" ⇒
          Response(Ok, headers = Seq("Content-Type" -> "application/xml"),
            body = "</xml>")
        case "application/json" ⇒
          Response(Ok, headers = Seq("Content-Type" -> "application/json"),
            body = "test : {}")
        case "text/html" ⇒
          Response(Ok, headers = Seq("Content-Type" -> "text/html"),
            body = "</html>")
      } getOrElse (Response(UnsupportedMediaType)))
  }

  onRequest {
    case r @ Authenticated(false) if privateResponder.isDefinedAt(r) ⇒
      reply(r.acceptedMimeTypes.collectFirst {
        case "application/xml" ⇒
          Response(Unauthorized, headers = Seq("Content-Type" -> "application/xml"),
            body = "<message>Invalid api key</message>")
        case "application/json" ⇒
          Response(Unauthorized, headers = Seq("Content-Type" -> "application/json"),
            body = "{'message' : 'Invalid api key'}")
        case "text/html" ⇒
          Response(Unauthorized, headers = Seq("Content-Type" -> "text/html"),
            body = "<form action='login' method='POST' ><input name='password'/><input type='submit' value='Login'></form>")
      } getOrElse (Response(UnsupportedMediaType)))
    case r ⇒
      (privateResponder orElse publicResponder)(r)
  }

  after { response ⇒
    val headers = response.headers ++ Map(
      "Server" -> "RestExampleSmoke/0.0.1",
      "Connection" -> "Close")
    Response(response.status, headers, response.body)
  }

}
