package smoke

import scala.util.Try
import java.nio.charset.Charset
import org.jboss.netty.util.CharsetUtil

trait Headers {
  val headers: Seq[(String, String)]

  def lastHeaderValue(header: String) =
    allHeaderValues(header) match {
      case list if !list.isEmpty ⇒ Some(list.last)
      case _                     ⇒ None
    }

  def allHeaderValues(header: String) = headers.filter(h ⇒ h._1.toLowerCase == header.toLowerCase) map { case (k, v) ⇒ v }

  def concatenateHeaderValues(header: String) = allHeaderValues(header) match {
    case x if x.isEmpty ⇒ None
    case s              ⇒ Some(s.mkString(","))
  }

  //Header Parsing

  //Sort accept header by given priority (q=?)
  lazy val acceptHeaders: Seq[String] =
    allHeaderValues("accept").map {
      _.split(";").toList match {
        case mt :: p :: Nil ⇒ (mt, p.split("q=").last.toFloat)
        case mt :: _        ⇒ (mt, 1.0f)
      }
    }.sortWith((a, b) ⇒ a._2 > b._2).map(_._1)

  lazy val contentType = lastHeaderValue("content-type")

  lazy val charset = contentType map { t ⇒
    t.split(";").filter(_.contains("charset")).map(_.split("=")).map(x ⇒ (x.last)).toSeq.headOption match {
      case Some(c) ⇒ Try(Charset.forName(c)).toOption.getOrElse(CharsetUtil.UTF_8)
      case None    ⇒ CharsetUtil.UTF_8
    }
  } getOrElse (CharsetUtil.UTF_8)

  lazy val userAgent = lastHeaderValue("user-agent")
}
