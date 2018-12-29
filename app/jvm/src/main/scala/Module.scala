import app.api.ScalaJsApiModule
import com.google.inject.AbstractModule
import app.common.CommonModule
import app.models.ModelsModule
import app.models.accounting.config.ConfigModule
import tools.ApplicationStartHook

final class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[ApplicationStartHook]).asEagerSingleton

    install(new CommonModule)
    install(new ConfigModule)
    install(new ModelsModule)
    install(new ScalaJsApiModule)
  }
}
