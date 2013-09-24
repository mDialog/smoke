package smoke

import akka.actor.{ Actor, Props }
import akka.util.ByteString
import java.io.{ File, FileInputStream, FileNotFoundException }

case class Asset(contentType: String, data: ByteString)

object StaticAssets {
  def apply(publicFolder: String) = Props(classOf[StaticAssets], publicFolder)
}

class StaticAssets(publicFolder: String) extends Actor {

  val config = context.system.settings.config

  private def getExtension(name: String) = {
    val dotIndex = name.lastIndexOf('.')

    if (dotIndex == -1) "" else name.substring(dotIndex + 1)
  }

  private def readFile(file: File) = {
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)

    in.read(bytes)
    in.close()

    ByteString(bytes)
  }

  private def loadAssets(folder: File): Seq[(String, Asset)] =
    folder.listFiles flatMap {
      case file if file.isFile ⇒
        val relativePath = file.getPath.drop(publicFolder.length)
        val extension = getExtension(file.getName)
        Seq(relativePath -> Asset(MimeType(extension), readFile(file)))

      case directory ⇒ loadAssets(directory)
    }

  private lazy val cachedAssets = loadAssets(new File(publicFolder)).toMap

  val loadAsset: String ⇒ Option[Asset] =
    if (config.getBoolean("smoke.static-assets.cache-assets"))
      (path: String) ⇒ cachedAssets.get(path)
    else
      (path: String) ⇒
        try {
          val file = new File(s"$publicFolder$path")
          val extension = getExtension(file.getName)
          Some(Asset(MimeType(extension), readFile(file)))
        } catch {
          case e: FileNotFoundException ⇒ None
        }

  if (config.getBoolean("smoke.static-assets.cache-assets-preload") && config.getBoolean("smoke.static-assets.cache-assets")) cachedAssets

  def receive = {
    case path: String ⇒
      loadAsset(path) match {
        case Some(asset) ⇒
          val data = RawData(asset.data.toArray)
          sender ! Response(Ok, Seq("Content-Type" -> asset.contentType), data)

        case None ⇒ sender ! Response(NotFound)
      }
  }
}
