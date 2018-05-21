package models.access

import api.ScalaJsApi.UpdateToken
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala2js.Converters._

/** Proxy for the server-side database. */
trait RemoteDatabaseProxy {
  def queryExecutor[E <: Entity: EntityType](): DbQueryExecutor.Async[E]

  def pendingModifications(): Future[Seq[EntityModification]]

  def persistEntityModifications(modifications: Seq[EntityModification]): PersistEntityModificationsResponse

  def getAndApplyRemotelyModifiedEntities(
      updateToken: Option[UpdateToken]): Future[GetRemotelyModifiedEntitiesResponse]

  case class PersistEntityModificationsResponse(queryReflectsModifications: Future[Unit],
                                                completelyDone: Future[Unit])
  case class GetRemotelyModifiedEntitiesResponse(changes: Seq[EntityModification],
                                                 nextUpdateToken: UpdateToken)
}
