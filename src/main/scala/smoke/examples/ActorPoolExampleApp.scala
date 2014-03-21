package smoke.examples

import smoke._

import akka.actor._
import akka.routing.RoundRobinPool
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

object NotFoundException extends Exception("Not found")

class PooledResponder extends Actor {
  def receive = {
    case GET(Path("/example")) ⇒
      Thread.sleep(1000)
      sender ! Response(Ok, body = "It took me a second to build this response.\n")
    case _ ⇒ sender ! Status.Failure(NotFoundException)
  }
}

object ActorPoolExampleApp extends SmokeApp {
  val smokeConfig = ConfigFactory.load().getConfig("smoke")
  val system = ActorSystem("ActorPoolExampleApp", smokeConfig)
  val executionContext = system.dispatcher
  val pool = system.actorOf(Props[PooledResponder].withRouter(RoundRobinPool(200)))
  implicit val timeout = Timeout(10.seconds)

  onRequest(pool ? _ mapTo manifest[Response])

  onError {
    case (_, NotFoundException) ⇒ Response(NotFound)
    case (_, e: Exception)      ⇒ Response(InternalServerError, body = e.getMessage)
  }

  after { response ⇒
    val headers = response.headers :+ ("Server", "ActorPoolExampleApp/0.0.1")
    Response(response.status, headers, response.body)
  }

  beforeShutdown {
    println("Shutdown in 5 s")
    Thread.sleep(5000)
  }

  afterShutdown {
    println("Shutdown complete!")
  }
}

