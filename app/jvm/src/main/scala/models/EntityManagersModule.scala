package models

import com.google.inject.AbstractModule
import tools.ApplicationStartHook
import models.accounting.config.ConfigModule

final class EntityManagersModule extends AbstractModule {
  override def configure() = {
    bind(classOf[User.Manager]).to(classOf[SlickUserManager])
  }
}
