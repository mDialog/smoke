package com.mdialog.smoke.mongrel2

import akka.actor._
import akka.dispatch.{ Future, Promise }
import akka.zeromq.ZeroMQExtension
import akka.zeromq.{ Connect, Frame, Listener, SocketType, ZMQMessage }

import com.mdialog.smoke._

class Mongrel2Server(receiveAddress: String, sendAddress: String) extends Server {
  val system = ActorSystem("SmokeMongrel2Server") 
  val handler = system.actorOf(Props(new Mongrel2Handler(receiveAddress, sendAddress)))
  
  println("Receiving requests on " + receiveAddress)
  println("Sending responses on " + sendAddress)
  
  def updateApplication = handler ! SetApplication(application)

  case class SetApplication(application: (Request) => Future[Response])

  class Mongrel2Handler(receiveAddress: String, sendAddress: String) 
    extends Actor {

    import context.dispatcher
    val system = ZeroMQExtension(context.system)
    
    val pullSocket = system.newSocket(SocketType.Pull, Connect(receiveAddress), Listener(self))
    val pubSocket = system.newSocket(SocketType.Pub, Connect(sendAddress))

    var application: (Request) => Future[Response] = { request =>
      Promise.successful(Response(ServiceUnavailable))
    }

    def send(request: Mongrel2Request, response: Response) = {
      val (sender, connection) = (request.sender, request.connection)
      val header = sender + " " + connection.length + ":" + connection + ","
      pubSocket ! ZMQMessage(Seq(Frame(header + " " + response.toMessage)))
      pubSocket ! ZMQMessage(Seq(Frame(header + " ")))
    }

    def log(request: Mongrel2Request, response: Response) =
      println(request.sender + " " + request.connection + " - " + response.statusCode + " " + response.statusMessage + " " + request.path)

    def receive = {
      case m: ZMQMessage => 
        val request = Mongrel2Request(m.payload(0))
        application(request) map { response =>
          send(request, response) 
          log(request, response)
        }

      case SetApplication(newApplication) => application = newApplication
    }  
  }
}
