package smoke

import java.io.{ File, FileInputStream, FileNotFoundException }
import scala.util.Try
import scala.io.Source

case class Asset(contentType: String, data: Array[Byte])

trait StaticAssets {
  val publicFolder: String
  val cacheAssets: Boolean = false

  private var cachedAssets = Map[String, Asset]()

  private def getExtension(name: String) = {
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex == -1) "" else name.substring(dotIndex + 1)
  }

  private def readFile(path: String) = {
    Option(this.getClass.getClassLoader.getResourceAsStream(path)) map {
      is ⇒
        try {
          Source.fromInputStream(is).map(_.toByte).toArray
        } finally {
          is.close()
        }
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
    val securedPath = path.split("/").filterNot {
      case ".." ⇒ true
      case "."  ⇒ true
      case _    ⇒ false
    } mkString ("/")
    getAsset(securedPath) match {
      case Some(asset) ⇒
        Response(Ok, Seq("Content-Type" -> asset.contentType), RawData(asset.data))
      case None ⇒
        Response(NotFound)
    }
  }
}
