package smoke

import java.net.URL
import java.io.{ BufferedInputStream, FileInputStream }
import scala.util.Try
import scala.io.Source

case class Asset(contentType: String, data: Array[Byte])

trait StaticAssets {
  val publicFolder: String
  val cacheAssets: Boolean = false

  val ApplicationPrefix =
    this.getClass.getClassLoader.getResource("").getPath()
  lazy val PublicFolderPrefix =
    this.getClass.getClassLoader.getResource(publicFolder).getPath()

  private var cachedAssets = Map[String, Asset]()

  private def getExtension(name: String) = {
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex == -1) "" else name.substring(dotIndex + 1)
  }

  private def isStaticAsset(r: URL) =
    r.getPath().startsWith(PublicFolderPrefix) ||
      (r.getProtocol() == "jar" &&
        r.getPath().stripPrefix("file:").startsWith(ApplicationPrefix))

  private def readFile(path: String) = {
    Option(this.getClass.getClassLoader.getResource(path)) match {
      case Some(r) if isStaticAsset(r) ⇒
        val is = r.openStream
        try {
          val bis = new BufferedInputStream(is)
          Some(Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray)
        } finally {
          is.close()
        }
      case _ ⇒
        None
    }
  }

  private def loadAsset(p: String): Option[Asset] = {
    val path = s"$publicFolder$p"
    readFile(path) map { bytes ⇒
      val extension = getExtension(path)
      Asset(MimeType(extension), bytes)
    }
  }

  private val getAsset: String ⇒ Option[Asset] =
    if (cacheAssets)
      (path: String) ⇒ cachedAssets.get(path).orElse {
        val asset = loadAsset(path)
        asset.map { a ⇒
          cachedAssets += path -> a
        }
        asset
      }
    else (path: String) ⇒ loadAsset(path)

  def responseFromAsset(path: String): Response = {
    getAsset(path) match {
      case Some(asset) ⇒
        Response(Ok, Seq("Content-Type" -> asset.contentType), RawData(asset.data))
      case None ⇒
        Response(NotFound)
    }
  }
}
