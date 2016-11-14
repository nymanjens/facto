package models

import collection.immutable.Seq
import models.manager.SlickEntityManager
import models.accounting._
import models.accounting.money.ExchangeRateMeasurements

object Tables {

  val allEntityManagers: Seq[SlickEntityManager[_, _]] =
    Seq(Users, Transactions, TransactionGroups, BalanceChecks, TagEntities, UpdateLogs, ExchangeRateMeasurements)
}
