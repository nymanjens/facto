package models

import models.access.DbResultSet
import models.accounting._
import models.modification.EntityType
import models.money.ExchangeRateMeasurement
import models.user.User

abstract class EntityAccess(implicit val userManager: User.Manager,
                            val balanceCheckManager: BalanceCheck.Manager,
                            val transactionManager: Transaction.Manager,
                            val transactionGroupManager: TransactionGroup.Manager,
                            val exchangeRateMeasurementManager: ExchangeRateMeasurement.Manager) {

  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]

  def newQuerySyncForUser(): DbResultSet.Sync[User]
}
