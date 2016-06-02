package models

import collection.immutable.Seq

import models.manager.EntityManager
import models.accounting.{Transactions, TransactionGroups, BalanceChecks, UpdateLogs}

object Tables {

  val allManagers: Seq[EntityManager[_]] = Seq(Users, Transactions, TransactionGroups, BalanceChecks, UpdateLogs)
}
