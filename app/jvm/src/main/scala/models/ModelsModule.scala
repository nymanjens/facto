package models

import com.google.inject.AbstractModule
import models.accounting._
import models.modification.{EntityModificationEntity, SlickEntityModificationEntityManager}
import models.money._
import models.money.{ExchangeRateMeasurement, SlickExchangeRateMeasurementManager}
import models.user.{SlickUserManager, User}

final class ModelsModule extends AbstractModule {
  override def configure() = {
    bind(classOf[EntityAccess]).to(classOf[SlickEntityAccess])

    bindSingleton(classOf[User.Manager], classOf[SlickUserManager])
    bindSingleton(classOf[BalanceCheck.Manager], classOf[SlickBalanceCheckManager])
    bindSingleton(classOf[Transaction.Manager], classOf[SlickTransactionManager])
    bindSingleton(classOf[TransactionGroup.Manager], classOf[SlickTransactionGroupManager])
    bindSingleton(classOf[ExchangeRateMeasurement.Manager], classOf[SlickExchangeRateMeasurementManager])
    bindSingleton(classOf[EntityModificationEntity.Manager], classOf[SlickEntityModificationEntityManager])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
