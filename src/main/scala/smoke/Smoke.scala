package smoke

import java.util.concurrent.Executors
import java.util.Date
import java.text.SimpleDateFormat
import com.typesafe.config.ConfigFactory

import akka.dispatch.{ Future, Promise }
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.util.duration._

import smoke.netty.NettyServer

trait Smoke extends App {
  implicit val config = ConfigFactory.load()
  implicit val system = ActorSystem("Smoke", config)
  implicit val dispatcher = system.dispatcher
  
  val timeoutDuration: Long = config.getMilliseconds("smoke.timeout")
  implicit val timeout = Timeout(timeoutDuration milliseconds)
    
  private var beforeFilter = { request: Request => request }
  private var responder = { request: Request => 
    Promise.successful(Response(ServiceUnavailable)).future
  }
  private var afterFilter = { response: Response => response }
  private var errorHandler: PartialFunction[Throwable, Response] = { 
    case t: Throwable => Response(InternalServerError, body = t.getMessage + "\n" + 
      t.getStackTrace.mkString("\n"))
  }
    
  private var shutdownHooks = List(() => {
    server.stop()
    system.shutdown()
  })
  
  private def application = 
    beforeFilter andThen responder andThen { f => 
      f recover(errorHandler) map afterFilter 
    }
  
  val server = new NettyServer
  
  def before(filter: (Request) => Request) { beforeFilter = filter }

  def after(filter: (Response) => Response) { afterFilter = filter }

  def onRequest(handler: (Request) => Future[Response]) { responder = handler }

  def onError(handler: PartialFunction[Throwable, Response]) { 
    errorHandler = handler orElse errorHandler
  }
  
  def beforeShutdown(hook: => Unit) { shutdownHooks = hook _ :: shutdownHooks }

  def afterShutdown(hook: => Unit) { shutdownHooks = shutdownHooks ::: List(hook _) }

  def reply(action: => Response) = Future(action)

  def reply(r: Response) = Promise.successful(r)
  
  def fail(e: Exception) = Promise.failed(e)

  abstract override def main(args: Array[String]) = {
    super.main(args)
    server.setApplication(application)
    
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run = shutdownHooks foreach { hook => hook() }
    }))
  }
}

trait Server {
  val log = { (request: Request, response: Response) =>
    val dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
    println("[" + dateFormat.format(new Date()) + "] " + request.ip + " " +
            response.statusCode + " " + request.path)
  }
  
  def setApplication(application: (Request) => Future[Response]): Unit
  
  def stop(): Unit
}

/**
 * Request Extractors
 *
 * Inspired lifted from Play2 Mini (https://github.com/typesafehub/play2-mini)
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

object FileExtension {
  def unapply(path: String): Option[String] = path.split('.').toList match {
    case List() => None
    case all => Some(all.last)
  }
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
