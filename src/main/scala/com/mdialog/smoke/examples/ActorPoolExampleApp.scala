package com.mdialog.smoke.examples

import com.mdialog.smoke._
import com.mdialog.smoke.netty.NettyServer

import akka.actor._
import akka.routing.RoundRobinRouter
import akka.pattern.ask

object NotFoundException extends Exception("Not found")
object UnauthorizedException extends Exception("Unauthorized")

class PooledResponder extends Actor {
  def receive = {
    case GET(Path("/test")) => 
      Thread.sleep(1000)
      sender ! Response(Ok, body="It took me a second to build this response.\n")
    case _ => Status.Failure(NotFoundException)
  }
}

object ActorPoolExampleApp extends App with Smoke {
  val server = new NettyServer
  val pool = system.actorOf(Props[PooledResponder].withRouter(RoundRobinRouter(5)))
  
  onRequest { r => pool ? r mapTo manifest[Response] }
  
  onError {
    case NotFoundException => Response(NotFound)
    case UnauthorizedException => Response(Unauthorized)
    case e: Exception => Response(InternalServerError, body = e.getMessage)
  }

  after { response =>
    val headers = response.headers + ("Server" -> "ActorPoolExampleApp/0.0.1")
    Response(response.status, headers, response.body)
  }
}


