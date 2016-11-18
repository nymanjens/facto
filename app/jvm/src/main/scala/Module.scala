import com.google.inject.AbstractModule
import tools.ApplicationStartHook
import models.accounting.config.ConfigModule
import models.EntityManagersModule

final class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[ApplicationStartHook]).asEagerSingleton

    install(new ConfigModule)
    install(new EntityManagersModule)
  }
}
