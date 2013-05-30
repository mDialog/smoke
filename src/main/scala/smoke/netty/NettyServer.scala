package smoke.netty

import com.typesafe.config.Config
import akka.actor._
import akka.dispatch.{ Future, Promise }
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

class NettyServer(implicit val config: Config, system: ActorSystem)
    extends Server
    with ConfigHelpers {

  val channelFactory = new NioServerSocketChannelFactory()
  val handler = new NettyServerHandler(log)

  case class BindInfo(bootstrap: ServerBootstrap, port: Int, protocol: String)

  val connectList: Seq[BindInfo] = bootstrap("smoke.ports", "smoke.port",
    ssl = config.getBooleanOption("smoke.https.enabled").getOrElse(false))

  var allChannels: ChannelGroup = new DefaultChannelGroup()

  def setApplication(application: (Request) ⇒ Future[Response]) {
    handler.setApplication(application)
  }

  def start() {
    println("Starting Netty:")
    connectList foreach { info ⇒
      try {
        val channel = info.bootstrap.bind(new InetSocketAddress(info.port))
        allChannels.add(channel)
        println("\taccepting %s connections on port %d".format(info.protocol, info.port))
      } catch {
        case e: Exception ⇒
          println("\tERROR - not listening on port " + info.port)
          throw e
      }
    }

    if (allChannels.isEmpty) {
      println("\tERROR - no ports configured to bind to")
      throw new NoPortsException
    }
  }

  def stop() {
    println("Stopping Netty:")
    allChannels.iterator map { channel ⇒
      channel.close.awaitUninterruptibly()
      println("\tchannel " + channel)
    }
    println("Netty no longer accepting HTTP connections")
    channelFactory.releaseExternalResources()
  }

  private def bootstrap(listPortsPath: String, singlePortPath: String, ssl: Boolean = false) = {
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

    val ports: List[Int] = config.getIntListOption(listPortsPath).getOrElse(
      config.getIntOption(singlePortPath).toList)

    ports map { p ⇒ BindInfo(bootstrap, p, protocol) }
  }
}

class NettyServerHandler(log: (Request, Response) ⇒ Unit)(implicit system: ActorSystem) extends SimpleChannelUpstreamHandler {
  import HttpHeaders.Names._
  import HttpHeaders.Values._

  implicit val dispatcher = system.dispatcher

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
        nettyResponse.setHeader(CONTENT_LENGTH, nettyResponse.getContent.readableBytes)
        nettyResponse.setHeader(CONNECTION, KEEP_ALIVE)
      }

      headers foreach { pair ⇒ nettyResponse.setHeader(pair._1, pair._2) }

      val channel = e.getChannel
      val future = channel.write(nettyResponse)

      if (!request.keepAlive || !HttpHeaders.isKeepAlive(nettyResponse)) {
        future.addListener(ChannelFutureListener.CLOSE)
      }

      log(request, response)
    } onFailure {
      case t: Throwable ⇒ throw t
    }
  }

  var application: (Request) ⇒ Future[Response] = { request ⇒
    Promise.successful(Response(ServiceUnavailable))
  }

  def setApplication(newApplication: (Request) ⇒ Future[Response]) {
    application = newApplication
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getChannel.close
  }
}
