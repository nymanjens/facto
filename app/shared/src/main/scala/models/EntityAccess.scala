package models

import models.access.DbResultSet
import models.accounting._
import models.modification.{EntityModification, EntityType}
import models.money.ExchangeRateMeasurement
import models.user.User

import scala.collection.immutable.Seq
import scala.concurrent.Future

abstract class EntityAccess(implicit val userManager: User.Manager,
                            val balanceCheckManager: BalanceCheck.Manager,
                            val transactionManager: Transaction.Manager,
                            val transactionGroupManager: TransactionGroup.Manager,
                            val exchangeRateMeasurementManager: ExchangeRateMeasurement.Manager) {

  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E]
}
