package app.models.accounting.config

import java.nio.file.Files
import java.nio.file.Paths

import hydro.common.ResourceFiles
import com.google.common.base.Throwables
import com.google.inject.AbstractModule
import com.google.inject.Provides
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess
import play.api.Logger

final class ConfigModule extends AbstractModule {

  val logger: Logger = Logger(this.getClass())

  override def configure() = {}

  @Provides()
  private[config] def config(playConfiguration: play.api.Configuration): Config = {
    try {
      // get configLocation
      val configLocation = playConfiguration.get[String]("app.accounting.configYamlFilePath")

      // get data
      val stringData = {
        if (Files.exists(Paths.get(configLocation))) {
          scala.io.Source.fromFile(configLocation).mkString
        } else {
          require(
            ResourceFiles.exists(configLocation),
            s"Could not find $configLocation as file or as resource",
          )
          ResourceFiles.read(configLocation)
        }
      }

      // parse data
      val constr = new CustomClassLoaderConstructor(getClass.getClassLoader)
      val yaml = new Yaml(constr)
      yaml.setBeanAccess(BeanAccess.FIELD)
      val configData = yaml.load(stringData).asInstanceOf[Parsable.Config]

      // convert to parsed config
      configData.parse
    } catch {
      case e: Throwable =>
        val stackTrace = Throwables.getStackTraceAsString(e)
        logger.error(s"Error when parsing accounting-config.yml: $stackTrace")
        throw e
    }
  }
}
