package smoke

import org.slf4j.LoggerFactory

import ch.qos.logback.classic.{ Logger, Level }
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{ ConsoleAppender, FileAppender }

import java.util.Date
import java.text.SimpleDateFormat
import java.io._

import com.typesafe.config.Config
import scala.concurrent.Future

trait Server {
  val config: Config

  protected lazy val accessLogger = LoggerFactory.getLogger("smoke.Server").asInstanceOf[Logger]
  protected lazy val errorLogger = LoggerFactory.getLogger("smoke.Server.error").asInstanceOf[Logger]
  
  val logDateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z")

  val log = { (request: Request, response: Response) ⇒
    val entry = request.ip + " - - " +
      "[" + logDateFormat.format(new Date()) + "] " +
      "\"" + request.method + " " + request.path + " " + request.version + "\" " +
      response.statusCode + " " + response.contentLength + " " + (System.currentTimeMillis - request.timestamp) + "ms"

    accessLogger.info(entry)
  }

  val errorLogVerbose = config.getBoolean("smoke.error-log-verbose")

  val errorLog = { (t: Throwable, peerSocketAddress: String, channelId: String) ⇒
    val entry = "[" + logDateFormat.format(new Date()) + "] [" +
      peerSocketAddress + "] [id " + channelId + "] " +
      (if (errorLogVerbose) {
        val sw = new StringWriter
        t.printStackTrace(new PrintWriter(sw))
        sw.toString
      } else t.getMessage)

    errorLogger.error(entry)
  }

  def setApplication(application: (Request) ⇒ Future[Response]): Unit

  def start(): Unit

  def stop(): Unit
}
