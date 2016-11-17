package models

import com.google.inject.AbstractModule
import tools.ApplicationStartHook
import models.accounting._

final class EntityManagersModule extends AbstractModule {
  override def configure() = {
    bindSingleton(classOf[User.Manager], classOf[SlickUserManager])
    bindSingleton(classOf[BalanceCheck.Manager], classOf[SlickBalanceCheckManager])
    bindSingleton(classOf[TagEntity.Manager], classOf[SlickTagEntityManager])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
