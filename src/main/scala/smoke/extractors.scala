package smoke

/**
 * Request Extractors
 *
 * Inspired lifted from Play2 Mini (https://github.com/typesafehub/play2-mini)
 * and Unfiltered (http://unfiltered.databinder.net/)
 */

object Path {
  def unapply(req: Request) = Some(req.path)
  def apply(req: Request) = req.path
}

object Seg {
  def unapply(req: Request): Option[List[String]] =
    unapply(req.path)

  def unapply(path: String): Option[List[String]] = {
    val seg = apply(path)
    if (seg.isEmpty) None else Some(seg)
  }

  def apply(req: Request): List[String] =
    apply(req.path)

  def apply(path: String): List[String] =
    path.split('/').filterNot(_.isEmpty).toList
}

object Filename {
  def unapply(req: Request): Option[(String, String)] =
    unapply(req.path)

  def unapply(path: String): Option[(String, String)] =
    getFilename(path).map { filename ⇒
      filename.lastIndexOf('.') match {
        case -1 ⇒ (filename, "")
        case 0  ⇒ ("", filename)
        case dot ⇒
          val split = filename.splitAt(dot)
          (split._1, split._2.tail)
      }
    }

  object base {
    def unapply(req: Request): Option[String] =
      unapply(req.path)

    def unapply(path: String): Option[String] =
      Filename.unapply(path).map { n ⇒
        n._1 match {
          case "" ⇒ None
          case x  ⇒ Some(x)
        }
      } flatten
  }

  object extension {
    def unapply(req: Request): Option[String] =
      unapply(req.path)

    def unapply(path: String): Option[String] =
      Filename.unapply(path).map { n ⇒
        n._2 match {
          case "" ⇒ None
          case x  ⇒ Some(x)
        }
      } flatten
  }

  private def getFilename(path: String): Option[String] =
    path.split('/').filterNot(_.isEmpty).lastOption
}

object Params {
  def unapply(req: Request) = Some(req.params)
  def apply(req: Request) = req.params
}

object Cookies {
  def unapply(req: Request) = Some(req.cookies)
  def apply(req: Request) = req.cookies
}

object ContentType {
  def unapply(req: Request) = req.contentType
  def apply(req: Request) = req.contentType
}

class Method(method: String) {
  def unapply(req: Request) =
    if (req.method.equalsIgnoreCase(method)) Some(req)
    else None
  def apply(req: Request) = req.method.equalsIgnoreCase(method)
}

object GET extends Method("GET")
object POST extends Method("POST")
object PUT extends Method("PUT")
object DELETE extends Method("DELETE")
object HEAD extends Method("HEAD")
object CONNECT extends Method("CONNECT")
object OPTIONS extends Method("OPTIONS")
object TRACE extends Method("TRACE")

object & { def unapply[A](a: A) = Some(a, a) }

