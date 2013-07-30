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
}
