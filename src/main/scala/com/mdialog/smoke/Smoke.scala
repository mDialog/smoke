package com.mdialog.smoke

import java.util.concurrent.Executors
import akka.dispatch.{ Future, Promise }
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

trait Smoke extends App {

  implicit val config = ConfigFactory.load()
  implicit val dispatcher = ActorSystem("Smoke", config).dispatcher
  
  private var beforeFilter = { request: Request => request } 
  private var responder = { request: Request => 
    Promise.successful(Response(ServiceUnavailable)).future
  }
  private var afterFilter = { response: Response => response }
  private var errorHandler: PartialFunction[Throwable, Response] = { 
    case e: Exception => Response(InternalServerError) 
  }
  
  private def application = 
    beforeFilter andThen responder andThen { f => 
      f recover(errorHandler) map afterFilter 
    }
  
  val server: Server
    
  def before(filter: (Request) => Request) { beforeFilter = filter }

  def after(filter: (Response) => Response) { afterFilter = filter }

  def onRequest(handler: (Request) => Future[Response]) { responder = handler }

  def onError(handler: PartialFunction[Throwable, Response]) { errorHandler = handler }

  abstract override def main(args: Array[String]) = {
    super.main(args)
    server.setApplication(application)
  }
}

trait Server {
  def setApplication(application: (Request) => Future[Response]): Unit
}

/**
 * Request Extractors
 *
 * Inspired by Play2-mini (https://github.com/typesafehub/play2-mini)
 * and Unfiltered (http://unfiltered.databinder.net/)
 */
 
object Path {
  def unapply(req: Request) = Some(req.path)
  def apply(req: Request) = req.path
}

object Seg {
  def unapply(path: String): Option[List[String]] = path.split("/").toList match {
    case "" :: rest => Some(rest) // skip a leading slash
    case all => Some(all)
  }
}
 
object Params {
  def unapply(req: Request) = Some(req.params)
}

class Method(method: String) {
  def unapply(req: Request) =
    if (req.method.equalsIgnoreCase(method)) Some(req)
    else None
}

object GET extends Method("GET")
object POST extends Method("POST")
object PUT extends Method("PUT")
object DELETE extends Method("DELETE")
object HEAD extends Method("HEAD")
object CONNECT extends Method("CONNECT")
object OPTIONS extends Method("OPTIONS")
object TRACE extends Method("TRACE")
 
object & { def unapply[A](a: A) = Some(a, a) }