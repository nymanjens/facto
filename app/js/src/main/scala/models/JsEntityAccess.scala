package models

import models.access.{DbResultSet, RemoteDatabaseProxy}
import models.accounting._
import models.modification.{EntityModification, EntityType}
import models.money.JsExchangeRateMeasurementManager
import models.user.JsUserManager

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class JsEntityAccess(implicit override val userManager: JsUserManager,
                           override val balanceCheckManager: JsBalanceCheckManager,
                           override val transactionManager: JsTransactionManager,
                           override val transactionGroupManager: JsTransactionGroupManager,
                           override val exchangeRateMeasurementManager: JsExchangeRateMeasurementManager,
                           remoteDatabaseProxy: RemoteDatabaseProxy)
    extends EntityAccess {

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType]() = remoteDatabaseProxy.newQuery()

  // **************** Setters ****************//
  def persistModifications(modifications: Seq[EntityModification]) =
    remoteDatabaseProxy.persistModifications(modifications)
  final def persistModifications(modifications: EntityModification*): Future[Unit] =
    persistModifications(modifications.toVector)
}
