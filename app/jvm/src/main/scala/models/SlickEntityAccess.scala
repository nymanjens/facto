package models

import com.google.inject._
import collection.immutable.Seq
import models.manager.SlickEntityManager
import models.accounting._
import models.accounting.money.SlickExchangeRateMeasurementManager

final class SlickEntityAccess @Inject()(implicit override val userManager: SlickUserManager,
                                        override val balanceCheckManager: SlickBalanceCheckManager,
                                        override val tagEntityManager: SlickTagEntityManager,
                                        override val transactionManager: SlickTransactionManager,
                                        override val transactionGroupManager: SlickTransactionGroupManager,
                                        override val updateLogManager: SlickUpdateLogManager,
                                        override val exchangeRateMeasurementManager: SlickExchangeRateMeasurementManager
                                       ) extends EntityAccess {

  val allEntityManagers: Seq[SlickEntityManager[_, _]] =
    Seq(
      userManager,
      transactionManager,
      transactionGroupManager,
      balanceCheckManager,
      tagEntityManager,
      updateLogManager,
      exchangeRateMeasurementManager)
}
