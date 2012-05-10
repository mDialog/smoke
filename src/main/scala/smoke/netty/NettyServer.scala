package smoke.netty

import com.typesafe.config.Config  
import akka.actor._  
import akka.dispatch.{ ExecutionContext, Future, Promise }  

import java.util.concurrent.Executors
import java.net.InetSocketAddress

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

import collection.JavaConversions._

import smoke._

class NettyServer(implicit config: Config) extends Server {
  val port = config.getInt("smoke.netty.port")
  val handler = new NettyServerHandler(log)
  val piplineFactory = new NettyServerPipelineFactory(handler)
  
  val bootstrap = new ServerBootstrap(
    new NioServerSocketChannelFactory(
      Executors.newCachedThreadPool,
      Executors.newCachedThreadPool));  
  bootstrap.setPipelineFactory(piplineFactory);  
  bootstrap.bind(new InetSocketAddress(port));
  
  println("Netty now accepting HTTP connections on port " + port.toString)
    
  def setApplication(application: (Request) => Future[Response]) {
    handler.setApplication(application)
  }  
}

class NettyServerPipelineFactory(handler: NettyServerHandler) 
  extends ChannelPipelineFactory {
  def getPipeline = {
    val p = Channels.pipeline

    p.addLast("decoder", new HttpRequestDecoder())
    p.addLast("aggregator", new HttpChunkAggregator(1048576))
    p.addLast("encoder", new HttpResponseEncoder())
    p.addLast("deflater", new HttpContentCompressor())
    p.addLast("handler", handler)
    p
  }
}

class NettyServerHandler(log: (Request, Response) => Unit) extends SimpleChannelUpstreamHandler {
  val executorService = Executors.newCachedThreadPool
  implicit val context = ExecutionContext.fromExecutor(executorService)
    
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val address = e.getRemoteAddress
    val request = NettyRequest(address, e.getMessage.asInstanceOf[HttpRequest])
    
    application(request) map { response =>
      val status = HttpResponseStatus.valueOf(response.statusCode)
      val headers = response.headers
      val body = response.body
      
      val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
      headers foreach { pair => nettyResponse.setHeader(pair._1, pair._2) } 
      nettyResponse.setContent(ChannelBuffers.copiedBuffer(body, CharsetUtil.UTF_8))
      
      val channel = e.getChannel
      channel.write(nettyResponse).addListener(ChannelFutureListener.CLOSE)
      
      log(request, response)    
    }
  } 
  
  var application: (Request) => Future[Response] = { request =>
    Promise.successful(Response(ServiceUnavailable))
  }  

  def setApplication(newApplication: (Request) => Future[Response]) { 
    application = newApplication 
  }
  
  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getChannel.close();
  }
}
