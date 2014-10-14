package smoke.netty

import com.typesafe.config.Config
import scala.concurrent.{ Future, ExecutionContext }

import grizzled.slf4j.Logging

import java.net.InetSocketAddress
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import smoke._
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.channel.group.DefaultChannelGroup
import collection.JavaConversions._

class NoPortsException extends Exception("No ports configured to bind to.")

class NettyServer(implicit val config: Config, ec: ExecutionContext)
    extends Server
    with Logging
    with ConfigHelpers {

  val channelFactory = new NioServerSocketChannelFactory()
  val handler = new NettyServerHandler(log, errorLog)

  implicit val dispatcher = ec

  case class BindInfo(bootstrap: ServerBootstrap, port: Int, protocol: String)

  val connectList: Seq[BindInfo] =
    bootstrap(config.getScalaIntList("http.ports"), ssl = false) ++
      bootstrap(config.getScalaIntList("https.ports"), ssl = true)

  var allChannels: ChannelGroup = new DefaultChannelGroup()

  def setApplication(application: (Request) ⇒ Future[Response]) {
    handler.setApplication(application)
  }

  def start() {
    logger.info("Starting Netty")
    connectList foreach { info ⇒
      try {
        val channel = info.bootstrap.bind(new InetSocketAddress(info.port))
        allChannels.add(channel)
        logger.info("Accepting %s connections on port %d".format(info.protocol, info.port))
      } catch {
        case e: Exception ⇒
          logger.error("Not listening on port " + info.port + " due to exception: " + e)
          throw e
      }
    }

    if (allChannels.isEmpty) {
      logger.error("No ports configured to bind to")
      throw new NoPortsException
    }
  }

  def stop() {
    logger.info("Stopping Netty")
    allChannels.iterator map { channel ⇒
      channel.close.awaitUninterruptibly()
      logger.info("channel " + channel)
    }
    logger.info("Netty no longer accepting HTTP connections")
    channelFactory.releaseExternalResources()
  }

  private def bootstrap(ports: Seq[Int], ssl: Boolean = false) =
    if (ports.isEmpty)
      Seq()
    else {
      val factory = new ChannelPipelineFactory {

        val sslHelper = if (ssl) Some(SSLHelper(config)) else None

        def getPipeline = {
          val p = Channels.pipeline

          if (ssl) {
            p.addLast("ssl", sslHelper.get.newHandler)
          }

          p.addLast("decoder", new HttpRequestDecoder)
          p.addLast("aggregator", new HttpChunkAggregator(1048576))
          p.addLast("encoder", new HttpResponseEncoder)
          p.addLast("deflater", new HttpContentCompressor)
          p.addLast("handler", handler)
          p
        }
      }

      val protocol = if (ssl) "HTTPS" else "HTTP"

      val bootstrap = new ServerBootstrap(channelFactory)
      bootstrap.setPipelineFactory(factory)

      ports map { p ⇒ BindInfo(bootstrap, p, protocol) }
    }
}

class NettyServerHandler(
    log: (Request, Response) ⇒ Unit,
    errorLog: (Throwable, String, String) ⇒ Unit)(implicit ec: ExecutionContext) extends SimpleChannelUpstreamHandler {
  import HttpHeaders.Names._
  import HttpHeaders.Values._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val address = e.getRemoteAddress
    val request = NettyRequest(address, e.getMessage.asInstanceOf[HttpRequest])

    application(request) map { response ⇒
      val status = HttpResponseStatus.valueOf(response.statusCode)
      val headers = response.headers
      val body = response.body
      val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)

      body match {
        case utf8: UTF8Data ⇒ nettyResponse.setContent(
          ChannelBuffers.copiedBuffer(utf8.data, CharsetUtil.UTF_8))
        case raw: RawData ⇒ nettyResponse.setContent(
          ChannelBuffers.copiedBuffer(raw.data))
      }

      if (request.keepAlive) {
        HttpHeaders.setContentLength(nettyResponse, nettyResponse.getContent.readableBytes)
        HttpHeaders.setKeepAlive(nettyResponse, true)
      }

      headers foreach { pair ⇒ HttpHeaders.addHeader(nettyResponse, pair._1, pair._2) }

      val channel = e.getChannel
      val future = channel.write(nettyResponse)

      if (!request.keepAlive || !HttpHeaders.isKeepAlive(nettyResponse)) {
        future.addListener(ChannelFutureListener.CLOSE)
      }

      log(request, response)
    } onFailure {
      case t: Throwable ⇒
        val peerSocketAddress = e.getRemoteAddress.toString
        errorLog(t, peerSocketAddress, e.getChannel.getId.toString)
        e.getChannel.close
    }
  }

  var application: (Request) ⇒ Future[Response] = { request ⇒
    Future.successful(Response(ServiceUnavailable))
  }

  def setApplication(newApplication: (Request) ⇒ Future[Response]) {
    application = newApplication
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val exception = e.getCause()
    val peerSocketAddress = e.getChannel.getRemoteAddress.toString
    errorLog(exception, peerSocketAddress, e.getChannel.getId.toString)
    e.getChannel.close
  }
}
