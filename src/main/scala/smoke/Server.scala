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

  protected lazy val accessLogger = {
    val logger = LoggerFactory.getLogger("smoke.Server")
      .asInstanceOf[Logger]

    logger.setAdditive(false)
    val context = logger.getLoggerContext

    config.getString("log-type") match {
      case "file" ⇒
        val fileEncoder = new PatternLayoutEncoder()
        fileEncoder.setContext(context)
        fileEncoder.setPattern("%message%n")
        fileEncoder.start()

        val logFile = config.getString("log-file")
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

      case "logback" ⇒

      case _ ⇒
        logger.setLevel(Level.OFF)
    }

    logger
  }

  protected lazy val errorLogger = {
    val logger = LoggerFactory.getLogger("smoke.Server.error")
      .asInstanceOf[Logger]

    logger.setAdditive(false)
    val context = logger.getLoggerContext

    config.getString("error-log-type") match {
      case "file" ⇒
        val fileEncoder = new PatternLayoutEncoder()
        fileEncoder.setContext(context)
        fileEncoder.setPattern("%message%n")
        fileEncoder.start()

        val logFile = config.getString("error-log-file")
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
      response.statusCode + " " + response.contentLength + " " + (System.currentTimeMillis - request.timestamp) + "ms " +
      "\"" + request.lastHeaderValue("referer").getOrElse("-") + "\" " +
      "\"" + request.lastHeaderValue("user-agent").getOrElse("-") + "\""

    accessLogger.info(entry)
  }

  val errorLogVerbose = config.getBoolean("error-log-verbose")

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
