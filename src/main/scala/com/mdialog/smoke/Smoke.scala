package com.mdialog.smoke

import akka.actor.ActorRef
import akka.dispatch.Future

trait Smoke {
  val server: Server
    
  def before(filter: (Request) => Request) =
    server.setBeforeFilter(filter)

  def after(filter: (Response) => Response) =
    server.setAfterFilter(filter)

  def onRequest(responder: (Request) => Future[Response]) =
    server.setResponder(responder)

  def onError(handler: PartialFunction[Throwable, Response]) =
    server.setErrorHandler(handler)
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