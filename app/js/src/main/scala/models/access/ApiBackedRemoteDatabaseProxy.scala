package models.access

import java.time.Duration

import api.ScalaJsApi.UpdateToken
import api.ScalaJsApiClient
import common.time.Clock
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[access] final class ApiBackedRemoteDatabaseProxy(implicit apiClient: ScalaJsApiClient, clock: Clock)
    extends RemoteDatabaseProxy {

  override def queryExecutor[E <: Entity: EntityType]() = new DbQueryExecutor.Async[E] {
    override def data(dbQuery: DbQuery[E]) = apiClient.executeDataQuery(dbQuery)
    override def count(dbQuery: DbQuery[E]) = apiClient.executeCountQuery(dbQuery)
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) =
    apiClient.persistEntityModifications(modifications)

  override def getAndApplyRemotelyModifiedEntities(updateToken: Option[UpdateToken]) = async {
    val response =
      await(apiClient.getEntityModifications(updateToken getOrElse clock.now.plus(Duration.ofDays(-1))))
    GetRemotelyModifiedEntitiesResponse(
      changes = response.modifications,
      nextUpdateToken = response.nextUpdateToken)
  }
}
