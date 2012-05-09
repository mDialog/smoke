package com.mdialog.smoke.examples

import com.mdialog.smoke._
import com.mdialog.smoke.netty.NettyServer
import akka.dispatch.{ Future, Promise }

object ExampleApp extends App with Smoke {
  val server = new NettyServer
    
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
