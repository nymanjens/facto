package models

import com.google.inject._
import collection.immutable.Seq
import models.manager.SlickEntityManager
import models.accounting._
import models.accounting.money.ExchangeRateMeasurements

final class SlickEntityAccess @Inject()(implicit override val userManager: SlickUserManager) extends EntityAccess {

  val allEntityManagers: Seq[SlickEntityManager[_, _]] =
    Seq(userManager, Transactions, TransactionGroups, BalanceChecks, TagEntities, UpdateLogs, ExchangeRateMeasurements)
}
