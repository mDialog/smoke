package smoke

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

class SessionManager(appSecret: String) {
  private val keySpec = new SecretKeySpec(appSecret.getBytes(), "HmacSHA1")
  private val mac = Mac.getInstance("HmacSHA1")
  mac.init(keySpec)

  private[smoke] val CookieSignSeparator = "--"

  private object SignedCookieValue {
    def unapply(signedCookieValue: String) =
      signedCookieValue.split(CookieSignSeparator).toList match {
        case cookie :: signature :: Nil if (sign(cookie) == signature) ⇒
          Some(cookie)
        case x ⇒ None
      }
  }

  private[smoke] def sign(s: String) = mac.doFinal(s.getBytes("utf-8")).map(c ⇒ f"$c%02x").mkString

  object Session {
    //TODO Session Expiration on client side
    def apply(values: Map[String, String]) = values.toSeq.map {
      case (k, v) ⇒ "Set-Cookie" -> (k + "=" + v + CookieSignSeparator + sign(v))
    }

    def unapply(req: Request) = {
      if (req.cookies.isEmpty) None
      else {
        Some(req.cookies.collect {
          case (key, SignedCookieValue(value)) ⇒ key -> value

        } toMap)
      }
    }

    def destroy(values: Map[String, String]) = values.toSeq.map {
      case (k, v) ⇒ "Set-Cookie" -> (k + "=")
    }
  }
}