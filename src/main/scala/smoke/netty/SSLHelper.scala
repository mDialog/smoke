package smoke.netty

import com.typesafe.config.Config
import javax.net.ssl.{ SSLContext, SSLEngine, KeyManagerFactory, TrustManagerFactory }
import java.security.KeyStore
import org.jboss.netty.handler.ssl.SslHandler
import java.io.FileInputStream

object SSLHelper extends ConfigHelpers {
  def apply(config: Config) = {

    config.getStringOption("smoke.https.debug") foreach { debugOpts ⇒
      System.setProperty("javax.net.debug", debugOpts)
    }

    val sslContext = makeSslContext(config)
    new SSLHelper(config, sslContext)
  }

  private def makeSslContext(config: Config) = {
    /* If no key store is specified, SSLContext will use the system properties
       javax.net.ssl.keyStore and javax.net.ssl.keyStorePassword.
    */
    val keyManagers = config.getStringOption("smoke.https.key-store") match {
      case None ⇒ null
      case Some(keyStorePath) ⇒
        val keyStorePw = config.getString("smoke.https.key-store-password")
        val ks = makeKeyStore(keyStorePath, keyStorePw)

        val kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
        kmf.init(ks, keyStorePw.toCharArray)
        kmf.getKeyManagers
    }

    /* If no trust store is specified, SSLContext tries to use the following
     defaults in order, using the password at system property value
     javax.net.ssl.trustStoreStorePassword:
     -the file referred to by the system property javax.net.ssl.trustStore
     -<java-home>/lib/security/jssecacerts
     -<java-home>/lib/security/cacerts
    */
    val trustManagers = config.getStringOption("smoke.https.trust-store") match {
      case None ⇒ null
      case Some(trustStorePath) ⇒
        val trustStorePw =
          config.getStringOption("smoke.https.trust-store-password").getOrElse("")
        val ks = makeKeyStore(trustStorePath, trustStorePw)

        val tmf =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
        tmf.init(ks)
        tmf.getTrustManagers
    }

    val sslContext = SSLContext.getInstance("TLS")

    sslContext.init(
      keyManagers,
      trustManagers,
      null //use the default secure random number generator
      )
    sslContext
  }

  private def makeKeyStore(storeLocation: String, storePassword: String) = {
    val ks = KeyStore.getInstance(KeyStore.getDefaultType)
    val ksStream = new FileInputStream(storeLocation)
    ks.load(ksStream, storePassword.toCharArray)
    ksStream.close()
    ks
  }
}

class SSLHelper(val config: Config, val sslContext: SSLContext) extends ConfigHelpers {

  def newHandler = {
    val engine = sslContext.createSSLEngine
    engine.setUseClientMode(false)

    config.getBooleanOption("smoke.https.use-client-auth") match {
      case Some(true) ⇒ engine.setNeedClientAuth(true)
      case _          ⇒
    }

    new SslHandler(engine)
  }
}
