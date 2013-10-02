package smoke

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

object Session {
  private val appSecret: String = ConfigFactory.load().getString("smoke.session.secret")

  private val keySpec = new SecretKeySpec(appSecret.getBytes(), "HmacSHA1");
  private val mac = Mac.getInstance("HmacSHA1");

  mac.init(keySpec);

  val CookieSignSeparator = "--"

  object SignedCookieValue {
    def unapply(signedCookieValue: String) =
      signedCookieValue.split(CookieSignSeparator).toList match {
        case cookie :: signature :: Nil if (sign(cookie) == signature) ⇒
          Some(cookie)
        case x ⇒ None
      }
  }

  def sign(s: String) = mac.doFinal(s.getBytes("utf-8")).map(c ⇒ f"$c%02x").mkString

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