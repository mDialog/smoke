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
  def unapply(path: String): Option[List[String]] = Some(path.split("/").toList filter { !_.isEmpty })
}

object Params {
  def unapply(req: Request) = Some(req.params)
}

object FileExtension {
  def unapply(path: String): Option[String] = path.split('.').toList match {
    case all if (path.contains(".")) ⇒ Some(all.last)
    case _                           ⇒ None
  }
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
