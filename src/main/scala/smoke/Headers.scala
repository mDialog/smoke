package smoke

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

}
