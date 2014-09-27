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

  def readFileFromStream( file :String ) :Array[Byte] = {
    val is = getClass.getResourceAsStream( file )

    val BUFFER_SIZE=64*1024
    val baos = new java.io.ByteArrayOutputStream(BUFFER_SIZE)
    val buffer = new Array[Byte](BUFFER_SIZE)
    var n = -1

    n = is.read( buffer, 0, BUFFER_SIZE )
    while( n > 0 ) {
      baos.write( buffer, 0, n )
      n = is.read( buffer, 0, BUFFER_SIZE )
    }
    is.close
    baos.toByteArray
  }

  private lazy val assetFolder = Option(this.getClass.getClassLoader.getResource(publicFolder)) match {
    case Some(url) ⇒ url.toString.split("file:").last.split("!").last
    case x         ⇒ throw new Exception("Error: static assets folder is not accessible: "+ x )
  }
  private lazy val bIsInJar =  Option(getClass.getClassLoader.getResource(publicFolder)) match {
    case Some(url) => url.toString .contains( "!/" )
    case _  => false
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

          val fileToGet=s"$assetFolder$path"
          val extension = getExtension(fileToGet)

          if( bIsInJar ) {
            Some( Asset(MimeType(extension), readFileFromStream(fileToGet) ) )
          } else {
            val file = new File(fileToGet)
            Some( Asset(MimeType(extension), readFile(file)))
          }

        } catch {
          case e: FileNotFoundException ⇒ None
        }

  if (cacheAssetsPreload && cacheAssets) cachedAssets


  def responseFromAsset(path: String, status :ResponseStatus = Ok, headers :Seq[(String,String)] = List() ): Response = {
    loadAsset(path) match {
      case Some(asset) ⇒
        Response(status, headers :+ ( "Content-Type" -> asset.contentType ), RawData(asset.data))
      case None ⇒
        Response(NotFound)
    }
  }
}
