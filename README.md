Smoke
======

[![Build Status](https://travis-ci.org/mDialog/smoke.svg?branch=master)](https://travis-ci.org/mDialog/smoke)

*Simple, asynchronous HTTP using scala.concurrent.Future*

A thin DSL for building simple, fast, scalable, asynchronous HTTP services with Scala.

## Using

In your build.sbt

    resolvers += "mDialog releases" at "http://mdialog.github.com/releases/"

    libraryDependencies += "com.mdialog" %% "smoke" % "2.0.1" //for akka 2.2.+

    libraryDependencies += "com.mdialog" %% "smoke" % "2.1.0" //for akka 2.3.+


Smoke 2.+ is made for use with Scala 2.10. If you're using an older
version of Scala, consider Smoke [0.3.0](https://github.com/mDialog/smoke/tree/5a0038099ff67113234fb8342a7328df6be1e9e4).

## Getting started

Smoke provides a DSL for building HTTP services using a simple request/response pattern, where each response is provided in a `scala.concurrent.Future`.

	import smoke._
	import com.typesafe.config.ConfigFactory

	object BasicExampleApp extends App {
  		val smoke = new BasicExampleSmoke
	}

	class BasicExampleSmoke extends Smoke {
  		val smokeConfig = ConfigFactory.load().getConfig("smoke")
  		val executionContext = scala.concurrent.ExecutionContext.global

  		onRequest {
    		case GET(Path("/example")) ⇒ reply {
      			Thread.sleep(1000)
      			Response(Ok, body = "It took me a second to build this response.\n")
    		}
    		case _ ⇒ reply(Response(NotFound))
  		}
	}

Be sure to check out the  [examples](https://github.com/mDialog/smoke/tree/master/src/main/scala/smoke/examples). Run them with sbt

    sbt run

## The Responder

The heart of any Smoke app is the responder function. Set it by supplying a function that accepts a `Request` and returns a `Future[Response]` to the `onRequest` method.

    onRequest {
      case GET(Path("/example")) => Future {
        Thread.sleep(1000)
        Response(Ok, body="It took me a second to build this response.\n")
      }
      case _ => reply(Response(NotFound))
    }

Smoke also provides a simple collection of request extractors based on the familiar set found in projects like [Play2 Mini](https://github.com/typesafehub/play2-mini) and [Unfiltered](http://unfiltered.databinder.net/Unfiltered.html). These can be used for pattern matching on `Request` objects.

    case GET(Path(Seg("resources" :: id :: Nil))) => ...
    case POST(Path("/resources")) & Params(p) => ...

To respond, you can use the `reply` method. Supply a function that returns a Response and it will be wrapped in a Future for you.

    case GET(Path("/example")) => reply {
      Thread.sleep(1000)
      Response(Ok, body="It took me a second.\n")
    }

If your request can be responded to quickly and immediately, you can pass a `Response` directly and it will be wrapped in a completed `Future`.

    case _ => reply(Response(NotFound))

Since a reply is just a `Future[Response]`, you can also get one from an [Akka](http://akka.io) actor.

    class Responder extends Actor {
      def receive = {
        case GET(Path("/example")) =>
          Thread.sleep(1000)
          sender ! Response(Ok, body="It took me a second.\n")
        case _ => sender ! Response(NotFound)
      }
    }

    object ActorExampleApp extends Smoke {
      val actor = system.actorOf(Props[Responder])

      onRequest (actor ? _ mapTo manifest[Response])
    }

To get a feel for the power of Scala's composable futures, [read the documentation](http://docs.scala-lang.org/overviews/core/futures.html).

### Responses

Responses are built using three parameters: a status code object, a `Seq[(String,String)]` of headers and a request body.

    Response(Ok, Seq(("Location", resource.location)), resource.toJson)

## Before/After filters

You can use the `before` and `after` filters to alter either the request prior to sending it to the responder or the response after it's been returned.

    after { resp =>
      val headers = resp.headers + ("Server" -> "ExampleApp/0.0.1")
      Response(resp.status, headers, resp.body)
    }

## Error handling

If your `Future[Response]` contains an exception, you can catch it and return an alternate response using the `onError` method. Any uncaught exceptions will return a 500.

	onRequest {
	  case _ => fail(NotFoundException)
	}

	onError {
	  case (request, NotFoundException) => Response(NotFound)
	}

This is especially useful when using a responder function composed from several Futures.

## Smoke workflow

Combining all those handlers, requests are processed like so:

    def application = withErrorHandling {
    		beforeFilter andThen responder andThen { _ map afterFilter }
  		}

## Graceful shutdown

Smoke will shutdown the server and `ActorSystem` when the process receives a `TERM` signal, from Ctrl-C or `kill <pid>` for instance. You can attach shutdown hooks both before and after this shutdown occurs.

    beforeShutdown {
      println("Getting ready to shutdown")
    }

    afterShutdown {
      println("No longer responding to requests.")
    }

## Using SmokeApp

Extending `SmokeApp` rather than `Smoke` creates a stand-alone application built around a Smoke HTTP server.

	import smoke._
	import com.typesafe.config.ConfigFactory

    object BasicExampleApp extends SmokeApp {
  	  val smokeConfig = ConfigFactory.load().getConfig("smoke")
  	  val executionContext = scala.concurrent.ExecutionContext.global

      onRequest {
        case GET(Path("/example")) => reply {
          Thread.sleep(1000)
          Response(Ok, body="It took me a second to build this response.\n")
        }
        case _ => reply(Response(NotFound))
      }
    }

## Logging
Smoke uses logback for all logging. Instead of having to define a logback configuration file for each application that uses Smoke, some convenient logging configuration options have been provided.

By default Smoke allows for some basic logging for all HTTP requests to either a file or to stdout. These can be specified in the smoke configuration by setting the following values:

    smoke {
      log-type = "stdout" # alternatively, set to "file" or "logback"
      log-file = "access.log" # used if log-type set to "file"
    }

If more advanced logging options using logback are desired, set the Smoke "log-type" configuration value to "logback" and use the logger name "smoke.Server" and "smoke.Server.error" to define custom behavior.

If no logging is desired set the "log-type" configuration to "none".

## SSL and Client Certificates

Smoke supports SSL, including optional use of client certificates. See the configuration section for more information.

## Configuration

Smoke uses [Typesafe Config Library](https://github.com/typesafehub/config). You can override any of the default configuration options using the `com.typesafe.Config` provided when creating your Smoke object.
The config passed to the smoke trait should be formatted as followed (without the smoke global object):

    smoke {
      log-type = "stdout" # alternatively, set to "file"
      log-file = "access.log" # used if log-type set to "file"

      error-log-type = "stdout" # alternatively, set to "file"
      error-log-file = "error.log" # used if log-type set to "file"
      error-log-verbose = false

      http {
        ##The default http port
        port = 7771

        #Multiple ports may be used by specifying a list, overriding the port setting
        #(Set empty to disable http)
        ports = [${smoke.http.port}]
      }

      session{
        secret=0sfi034nrosd23kaldasl
      }

      https {

        #The ports on which run as https (leave empty to disable https)
        ports = []

        # Server Authentication

        # The location of the jks format key store to be used
        # If not provided, the system property javax.net.ssl.keyStore is used
        key-store = "src/test/resources/ssl/test.jks"

        # The password for the key store.
        # If not provided, the system property javax.net.ssl.keyStorePassword is used
        key-store-password = "test-password"

        # Client Authentication

        # Set to true to enable SSL client certificates (2 way handshake)
        use-client-auth = false

        # The location of the jks format trust store to be used
        # If not provided, the system property javax.net.ssl.trustStore is used
        trust-store = "src/test/resources/ssl/test.jks"

        # The password for the trust store.
        # If not provided, the system property javax.net.ssl.trustStorePassword is used
        trust-store-password = "test-password"

        # Debug ssl, as per the javax.net.debug system property
        ## debug = "all"
      }

      static-assets {
        cache-assets = true
        cache-assets-preload = false

        public-dir = "public"
      }
    }

## Try it out

Clone the repository, run one of the sample apps:

    sbt run

Make requests:

    curl -i http://localhost:7771/example

## Testing

Unit testing components of your application that interact with Smoke is made easier using the provided TestRequest class, which inherits from the Request trait.

    case class TestRequest(uriString: String,
                           method: String = "GET",
                           headers: Seq[(String, String)] = Seq.empty,
                           body: String = "",
                           keepAlive: Boolean = true) extends Request


Use this class to test your application's responder function. You can test a Smoke instance by instantiating it and shutting it down inside a test. Invoke the application method directly passing it a TestRequest then write assertions against the resulting response.

    import org.scalatest.{ FunSpec, BeforeAndAfterAll }

    import scala.concurrent.Await
    import scala.concurrent.duration._

    import smoke._
    import smoke.test._

    class BasicExampleAppTest extends FunSpec with BeforeAndAfterAll {

      val app = new BasicExampleSmoke

      override def afterAll { app.shutdown() }

      describe("GET /example") {
        it("should respond with 200") {
          val request = TestRequest("/example")
          val response = Await.result(app.application(request), 2 seconds)
          assert(response.status === Ok)
        }
      }

      describe("POST /unknown-path") {
        it("should respond with 404") {
          val request = TestRequest("/unknown-path", "POST")
          val response = Await.result(app.application(request), 2 seconds)
          assert(response.status === NotFound)
        }
      }
    }

This is the same way Smoke processes requests while your app is running.

## Documentation

Read the API documentation here: [http://mdialog.github.com/api/smoke-2.1.0/](http://mdialog.github.com/api/smoke-2.1.0/)

## Mailing list

Send questions, comments or discussion topics to the mailing list <smoke@librelist.com>.

## License

This project is released under the Apache License v2, for more details see the 'LICENSE' file.

## Contributing

Fork the project, add tests if possible and send a pull request.

Unsure of where to start? Pick a TODO, or consider one of the following contributions: more Request extractors, better documentation, additional server backends, DSL improvements.

## Contributors

Vikraman Choudhury, Chris Dinn, David Harcombe, Gaetan Hervouet, Sebastian Hubbard, Matt MacAulay, Arron Norwell, Sana Tapal

**©2014 mDialog Corp. All rights reserved.**
