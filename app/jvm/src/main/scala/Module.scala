import com.google.inject.AbstractModule
import tools.ApplicationStartHook
import models.accounting.config.ConfigModule
import common.CommonModule
import models.EntityManagersModule

final class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[ApplicationStartHook]).asEagerSingleton

    install(new CommonModule)
    install(new ConfigModule)
    install(new EntityManagersModule)
  }
}
