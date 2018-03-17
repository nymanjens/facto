package models.access

import api.ScalaJsApi.UpdateToken
import api.ScalaJsApiClient
import common.LoggingUtils.logExceptions
import common.ScalaUtils.visibleForTesting
import common.time.Clock
import models.Entity
import models.access.JsEntityAccess.Listener
import models.modification.{EntityModification, EntityType}
import models.user.User
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[access] final class ApiBackedRemoteDatabaseProxy(implicit apiClient: ScalaJsApiClient, clock: Clock)
    extends RemoteDatabaseProxy {

  override def queryExecutor[E <: Entity: EntityType]() = new DbQueryExecutor.Async[E] {
    override def data(dbQuery: DbQuery[E]) = apiClient.executeDataQuery(dbQuery)
    override def count(dbQuery: DbQuery[E]) = apiClient.executeCountQuery(dbQuery)
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) =
    apiClient.persistEntityModifications(modifications)

  override def getAndApplyRemotelyModifiedEntities(updateToken: Option[UpdateToken]) = async {
    val response = await(apiClient.getEntityModifications(updateToken getOrElse clock.now))
    GetRemotelyModifiedEntitiesResponse(
      changes = response.modifications,
      nextUpdateToken = response.nextUpdateToken)
  }
}
