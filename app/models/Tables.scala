package models

import collection.immutable.Seq
import models.manager.EntityManager
import models.accounting._

object Tables {

  val allEntityManagers: Seq[EntityManager[_, _]] =
    Seq(Users, Transactions, TransactionGroups, BalanceChecks, TagEntities, UpdateLogs)
}
