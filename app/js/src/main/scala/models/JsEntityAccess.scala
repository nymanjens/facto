package models

import api.ScalaJsApi.GetInitialDataResponse
import models.access.{DbQueryExecutor, DbResultSet, RemoteDatabaseProxy}
import models.accounting._
import models.modification.{EntityModification, EntityType}
import models.money.JsExchangeRateMeasurementManager
import models.user.{JsUserManager, User}

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class JsEntityAccess(implicit override val userManager: JsUserManager,
                           override val balanceCheckManager: JsBalanceCheckManager,
                           override val transactionManager: JsTransactionManager,
                           override val transactionGroupManager: JsTransactionGroupManager,
                           override val exchangeRateMeasurementManager: JsExchangeRateMeasurementManager,
                           remoteDatabaseProxy: RemoteDatabaseProxy,
                           getInitialDataResponse: GetInitialDataResponse)
    extends EntityAccess {

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType]() = remoteDatabaseProxy.newQuery()

  override def newQuerySyncForUser() =
    DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(getInitialDataResponse.allUsers))

  // **************** Setters ****************//
  def persistModifications(modifications: Seq[EntityModification]) =
    remoteDatabaseProxy.persistModifications(modifications)
  final def persistModifications(modifications: EntityModification*): Future[Unit] =
    persistModifications(modifications.toVector)
}
