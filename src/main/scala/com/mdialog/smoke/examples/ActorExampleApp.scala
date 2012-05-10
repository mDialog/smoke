package com.mdialog.smoke.examples

import com.mdialog.smoke._
import com.mdialog.smoke.netty.NettyServer

import akka.actor._
import akka.pattern.ask

class Responder extends Actor {
  def receive = {
    case GET(Path("/test")) => 
      Thread.sleep(1000)
      sender ! Response(Ok, body="It took me a second to build this response.\n")
    case _ => sender ! Response(NotFound)
  }
}

object ActorExampleApp extends App with Smoke {
  val server = new NettyServer
  val actor = system.actorOf(Props[Responder])
  
  onRequest { r => actor ? r mapTo manifest[Response] }

  after { response =>
    val headers = response.headers + ("Server" -> "ActorExampleApp/0.0.1")
    Response(response.status, headers, response.body)
  }
}


