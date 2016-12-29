package models

import com.google.inject._
import collection.immutable.Seq
import models.manager.SlickEntityManager
import models.accounting._
import models.accounting.money.SlickExchangeRateMeasurementManager

final class SlickEntityAccess @Inject()(implicit override val userManager: SlickUserManager,
                                        override val balanceCheckManager: SlickBalanceCheckManager,
                                        override val transactionManager: SlickTransactionManager,
                                        override val transactionGroupManager: SlickTransactionGroupManager,
                                        override val exchangeRateMeasurementManager: SlickExchangeRateMeasurementManager,
                                        val entityModificationEntityManager: SlickEntityModificationEntityManager,
                                        val tagEntityManager: SlickTagEntityManager,
                                        val updateLogManager: SlickUpdateLogManager
                                       ) extends EntityAccess {

  val allEntityManagers: Seq[SlickEntityManager[_, _]] =
    Seq(
      userManager,
      transactionManager,
      transactionGroupManager,
      balanceCheckManager,
      exchangeRateMeasurementManager,
      entityModificationEntityManager,
      tagEntityManager,
      updateLogManager)
}
