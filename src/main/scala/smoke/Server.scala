package smoke

import org.slf4j.LoggerFactory

import ch.qos.logback.classic.{ Logger, Level }
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{ ConsoleAppender, FileAppender }

import java.util.Date
import java.text.SimpleDateFormat

import com.typesafe.config.Config
import scala.concurrent.Future

trait Server {
  val config: Config

  protected lazy val accessLogger = {
    val logger = LoggerFactory.getLogger("smoke.Server")
      .asInstanceOf[Logger]

    logger.setAdditive(false)
    val context = logger.getLoggerContext

    config.getString("smoke.log-type") match {
      case "file" ⇒
        val fileEncoder = new PatternLayoutEncoder()
        fileEncoder.setContext(context)
        fileEncoder.setPattern("%message%n")
        fileEncoder.start()

        val logFile = config.getString("smoke.log-file")
        val fileAppender = new FileAppender[ILoggingEvent]();
        fileAppender.setContext(context);
        fileAppender.setEncoder(fileEncoder);
        fileAppender.setFile(logFile)
        fileAppender.start();

        logger.addAppender(fileAppender)

      case "stdout" ⇒
        val consoleEncoder = new PatternLayoutEncoder()
        consoleEncoder.setContext(context)
        consoleEncoder.setPattern("%message%n")
        consoleEncoder.start()

        val consoleAppender = new ConsoleAppender[ILoggingEvent]();
        consoleAppender.setContext(context);
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.start();

        logger.addAppender(consoleAppender)

      case _ ⇒
    }

    logger
  }

  val logDateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z")

  val log = { (request: Request, response: Response) ⇒
    val entry = request.ip + " - - " +
      "[" + logDateFormat.format(new Date()) + "] " +
      "\"" + request.method + " " + request.path + " " + request.version + "\" " +
      response.statusCode + " " + response.contentLength + " " + (System.currentTimeMillis - request.timestamp) + "ms"

    accessLogger.info(entry)
  }

  def setApplication(application: (Request) ⇒ Future[Response]): Unit

  def start(): Unit

  def stop(): Unit
}
