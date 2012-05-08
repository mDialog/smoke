package com.mdialog.smoke.examples

import com.mdialog.smoke._
import com.mdialog.smoke.mongrel2.Mongrel2Server
import akka.dispatch.{ Future, Promise }
import akka.actor.ActorSystem

object ExampleApp extends App with Smoke {
  
  val server = new Mongrel2Server
    
  onRequest {
    case GET(Path("/test")) => Future {
      Thread.sleep(1000)
      Response(Ok, body="It took me a second to build this response.\n")
    }
    case _ => Promise.successful(Response(NotFound))
  }
  
  onError {
    case e: Exception => Response(InternalServerError, body = e.getMessage)
  }

  after { response =>
    val headers = response.headers + ("Server" -> "Smoke Example App/0.0.1")
    Response(response.status, headers, response.body)
  }
  
}
