package smoke

import java.io.{ File, FileInputStream, FileNotFoundException }

case class Asset(contentType: String, data: Array[Byte])

trait StaticAssets {
  val publicFolder: String
  val cacheAssets: Boolean = false
  val cacheAssetsPreload: Boolean = false

  private def getExtension(name: String) = {
    val dotIndex = name.lastIndexOf('.')

    if (dotIndex == -1) "" else name.substring(dotIndex + 1)
  }

  private def readFile(file: File) = {
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)

    in.read(bytes)
    in.close()

    bytes
  }

  private lazy val assetFolder = Option(this.getClass.getClassLoader.getResource(publicFolder)) match {
    case Some(url) ⇒ url.toString.split("file:").last
    case _         ⇒ throw new Exception("Error: static assets folder is not accessible")
  }

  private def loadAssets(folder: File): Seq[(String, Asset)] =
    folder.exists match {
      case true ⇒
        folder.listFiles flatMap {
          case file if file.isFile ⇒
            val relativePath = file.getPath.drop(assetFolder.length)
            val extension = getExtension(file.getName)
            Seq(relativePath -> Asset(MimeType(extension), readFile(file)))

          case directory ⇒ loadAssets(directory)
        }
      case false ⇒ Seq.empty
    }

  private lazy val cachedAssets = loadAssets(new File(assetFolder)).toMap

  private val loadAsset: String ⇒ Option[Asset] =
    if (cacheAssets)
      (path: String) ⇒ cachedAssets.get(path)
    else
      (path: String) ⇒
        try {
          val file = new File(s"$assetFolder$path")
          val extension = getExtension(file.getName)
          Some(Asset(MimeType(extension), readFile(file)))
        } catch {
          case e: FileNotFoundException ⇒ None
        }

  if (cacheAssetsPreload && cacheAssets) cachedAssets

  def responseFromAsset(path: String): Response = {
    loadAsset(path) match {
      case Some(asset) ⇒
        Response(Ok, Seq("Content-Type" -> asset.contentType), RawData(asset.data))
      case None ⇒
        Response(NotFound)
    }
  }
}
