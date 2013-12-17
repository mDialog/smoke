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
  def unapply(req: Request): Option[List[String]] = {
    unapply(req.path)
  }
  def unapply(path: String): Option[List[String]] = {
    val segs = path.split('/').filterNot(_.isEmpty)
    val heads = segs.take(segs.size - 1)
    Some((heads ++ segs.last.split('.')).toList)
  }
}

object Filename {
  def unapply(req: Request): Option[String] = {
    unapply(req.path)
  }
  def unapply(path: String): Option[String] = path.split('/').filterNot(_.isEmpty).lastOption
}

object FileExtension {
  def unapply(req: Request): Option[String] = {
    unapply(req.path)
  }
  def unapply(path: String): Option[String] = {
    val segs = path.split('/').filterNot(_.isEmpty)
    if (!segs.last.contains('.')) None
    else segs.last.split('.').tail.lastOption
  }
}

object ContentType {
  def unapply(req: Request): Option[String] = {
    FileExtension.unapply(req.path).map(MimeType(_)).orElse(req.allHeaderValues("accept").headOption)
  }
}

object Params {
  def unapply(req: Request) = Some(req.params)
}

class Method(method: String) {
  def unapply(req: Request) =
    if (req.method.equalsIgnoreCase(method)) Some(req)
    else None
  def apply(req: Request) = req.method.equalsIgnoreCase(method)
}

object Cookies {
  def unapply(req: Request) = Some(req.cookies)
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

