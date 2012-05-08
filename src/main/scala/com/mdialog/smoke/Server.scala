package com.mdialog.smoke

import java.util.concurrent.Executors
import akka.dispatch.{ ExecutionContext, Future, Promise }

trait Server {
  val executorService = Executors.newFixedThreadPool(10) 
  implicit val context = ExecutionContext.fromExecutor(executorService)
  
  private var beforeFilter = { request: Request => request } 
  private var responder = { request: Request => 
    Promise.successful(Response(ServiceUnavailable)).future
  }
  private var afterFilter = { response: Response => response }
  private var errorHandler: PartialFunction[Throwable, Response] = { 
    case e: Exception => Response(InternalServerError) 
  }
  
  protected def updateApplication: Unit

  protected def application = 
    beforeFilter andThen responder andThen { f => 
      f recover(errorHandler) map afterFilter 
    }

  def setBeforeFilter(filter: (Request) => Request) {
    beforeFilter = filter
    updateApplication
  }
  
  def setResponder(newResponder: (Request) => Future[Response]) {
    responder = newResponder
    updateApplication
  }
  
  def setAfterFilter(filter: (Response) => Response) {
    afterFilter = filter
    updateApplication
  }
  
  def setErrorHandler(handler: PartialFunction[Throwable, Response]) {
    errorHandler = handler
    updateApplication
  }
}