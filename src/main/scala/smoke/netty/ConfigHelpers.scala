package smoke.netty

import com.typesafe.config.{ Config, ConfigException }
import collection.JavaConversions._

trait ConfigHelpers {
  implicit def config2Boxed(config: Config) = new BoxedConfig(config)
}

class BoxedConfig(val config: Config) {
  private def optionallyGet[T](code: ⇒ T): Option[T] =
    try {
      Some(code)
    } catch {
      case _: ConfigException.Missing ⇒ None
    }

  def getIntListOption(path: String) =
    optionallyGet(config.getIntList(path).toList.asInstanceOf[List[Int]])

  def getScalaIntList(path: String) =
    config.getIntList(path).toList.asInstanceOf[List[Int]]

  def getIntOption(path: String) = optionallyGet(config.getInt(path))

  def getStringOption(path: String) = optionallyGet(config.getString(path))

  def getBooleanOption(path: String) = optionallyGet(config.getBoolean(path))
}
