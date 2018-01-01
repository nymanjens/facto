package models

import api.ScalaJsApi.GetInitialDataResponse
import models.access.{DbQueryExecutor, DbResultSet, RemoteDatabaseProxy}
import models.modification.{EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class JsEntityAccess(implicit remoteDatabaseProxy: RemoteDatabaseProxy,
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
