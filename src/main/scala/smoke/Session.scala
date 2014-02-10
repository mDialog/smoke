package smoke

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

class SessionManager(appSecret: String) {
  val mac = Mac.getInstance("HmacSHA1")
  mac.init(new SecretKeySpec(appSecret.getBytes(), "HmacSHA1"))

  private[smoke] val CookieSignSeparator = "--"
  private[smoke] def extractValids(signedCookieValue: String): Option[String] = {
    val values = signedCookieValue.split(CookieSignSeparator)
    if (values.size > 1 && sign(values(0)) == values(1)) {
      Some(values(0))
    } else None
  }
  private[smoke] def sign(s: String): String =
    mac.doFinal(s.getBytes("utf-8")).map(c ⇒ f"$c%02x").mkString

  object Session {
    //TODO Session Expiration on client side
    def apply(values: Map[String, String]) = values.toSeq.map {
      case (k, v) ⇒ "Set-Cookie" -> (k + "=" + v + CookieSignSeparator + sign(v))
    }

    def unapply(req: Request): Option[Map[String, String]] = {
      if (req.cookies.isEmpty) None
      else {
        Some(req.cookies.map {
          case (k, v) ⇒ k -> extractValids(v)
        } collect {
          case (k, Some(v)) ⇒ (k -> v)
        })
      }
    }

    def destroy(values: Map[String, String]) = values.toSeq.map {
      case (k, v) ⇒ "Set-Cookie" -> (k + "=")
    }
  }
}