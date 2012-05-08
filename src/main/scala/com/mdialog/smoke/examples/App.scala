package com.mdialog.smoke.examples

import com.mdialog.smoke._
import com.mdialog.smoke.netty.NettyServer

import java.util.concurrent.Executors
import akka.dispatch.{ ExecutionContext, Future, Promise }
import akka.util.Timeout
import akka.util.duration._

object ExampleApp extends App with Smoke {

  val executorService = Executors.newCachedThreadPool
  implicit val context = ExecutionContext.fromExecutor(executorService)
  
  //implicit val timeout = Timeout(2 seconds)
  
  val server = new NettyServer(7771)
  
  onRequest {
    case GET(Path("/test")) => Future {
      Thread.sleep(1000)
      Response(Ok, body="It took me a second to build this response.\n")
    }
    case _ => Promise.successful(Response(NotFound))
  }

  after { response =>
    val headers = response.headers + ("Server" -> "Example App/0.0.1")
    Response(response.status, headers, response.body)
  }
  
}
