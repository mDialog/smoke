package smoke.netty

import com.typesafe.config.ConfigFactory
import org.scalatest._

class SSLHelperTest extends FunSpec {

  describe("SSLHelper error cases") {

    it("should throw an exception if the key store password is wrong") {
      val config = ConfigFactory.parseString(
        """smoke.https {
             enabled = true
             key-store = "src/test/resources/ssl/test.jks"
             key-store-password = "wrong-password"
           } """)

      intercept[Exception] { SSLHelper(config) }
    }

    describe("client certificates") {

      it("should throw an exception if the trust store does not exist") {
        val config = ConfigFactory.parseString(
          """smoke.https {
               enabled = true
               key-store = "src/test/resources/ssl/test.jks"
               key-store-password = "test-password"

               use-client-auth = true
               trust-store = "non-existent"
               trust-store-password = "123"
            } """)

        intercept[Exception] { SSLHelper(config) }
      }

      it("should throw an exception if the trust store password is wrong") {
        val config = ConfigFactory.parseString(
          """smoke.https {
               enabled = true
               key-store = "src/test/resources/ssl/test.jks"
               key-store-password = "test-password"

               use-client-auth = true
               trust-store = "src/test/resources/ssl/test.jk"
               trust-store-password = "wrong-password"
             } """)

        intercept[Exception] { SSLHelper(config) }
      }
    }
  }

  describe("the produced SSLEngine") {

    it("should be in server mode") {
      val config = ConfigFactory.parseString(
        """smoke.https {
               enabled = true
               key-store = "src/test/resources/ssl/test.jks"
               key-store-password = "test-password"

               use-client-auth = true
               trust-store = "src/test/resources/ssl/test.jks"
               trust-store-password = "test-password"
          } """)

      val engine = SSLHelper(config).newHandler.getEngine

      assert(engine.getUseClientMode === false)
    }

    it("should require client auth if configured") {
      val config = ConfigFactory.parseString(
        """smoke.https {
               enabled = true
               key-store = "src/test/resources/ssl/test.jks"
               key-store-password = "test-password"

               use-client-auth = true
               trust-store = "src/test/resources/ssl/test.jks"
               trust-store-password = "test-password"
          } """)

      val engine = SSLHelper(config).newHandler.getEngine

      assert(engine.getNeedClientAuth === true)
    }

    it("should not require client auth if not configured") {
      val config = ConfigFactory.parseString(
        """smoke.https {
               enabled = true
               key-store = "src/test/resources/ssl/test.jks"
               key-store-password = "test-password"
          } """)

      val engine = SSLHelper(config).newHandler.getEngine

      assert(engine.getNeedClientAuth === false)
    }
  }
}
