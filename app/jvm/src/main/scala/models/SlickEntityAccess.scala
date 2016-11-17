package models

import com.google.inject._
import collection.immutable.Seq
import models.manager.SlickEntityManager
import models.accounting._
import models.accounting.money.ExchangeRateMeasurements

final class SlickEntityAccess @Inject()(implicit override val userManager: SlickUserManager,
                                        override val balanceCheckManager: SlickBalanceCheckManager,
                                        override val tagEntityManager: SlickTagEntityManager) extends EntityAccess {

  val allEntityManagers: Seq[SlickEntityManager[_, _]] =
    Seq(userManager, Transactions, TransactionGroups, balanceCheckManager, tagEntityManager, UpdateLogs, ExchangeRateMeasurements)
}
