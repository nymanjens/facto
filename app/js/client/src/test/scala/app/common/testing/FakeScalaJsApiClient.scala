package app.common.testing

import app.api.ScalaJsApi.GetAllEntitiesResponse
import app.api.ScalaJsApi.UserPrototype
import app.api.ScalaJsApiClient
import app.models.modification.EntityModification
import app.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryExecutor

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class FakeScalaJsApiClient extends ScalaJsApiClient {

  private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
  private val upsertedUserPrototypes: mutable.Buffer[UserPrototype] = mutable.Buffer()

  // **************** Implementation of ScalaJsApiClient trait ****************//
  override def getInitialData() = ???

  override def getAllEntities(types: Seq[EntityType.any]) = Future.successful {
    GetAllEntitiesResponse(
      entitiesMap = {
        for (entityType <- types) yield {
          entityType -> modificationsBuffer.getAllEntitiesOfType(entityType)
        }
      }.toMap,
      nextUpdateToken = modificationsBuffer.nextUpdateToken
    )
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) = {
    modificationsBuffer.addModifications(modifications)
    Future.successful((): Unit)
  }

  override def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]) =
    queryExecutor(dbQuery.entityType).map(_.data(dbQuery))
  override def executeCountQuery(dbQuery: DbQuery[_ <: Entity]) = {
    def internal[E <: Entity](dbQuery: DbQuery[E]): Future[Int] =
      queryExecutor(dbQuery.entityType).map(_.count(dbQuery))
    internal(dbQuery)
  }
  private def queryExecutor[E <: Entity](
      implicit entityType: EntityType[E]): Future[DbQueryExecutor.Sync[E]] = async {
    val entities = await(getAllEntities(Seq(entityType))).entities(entityType)
    DbQueryExecutor.fromEntities(entities)
  }

  override def upsertUser(userPrototype: UserPrototype): Future[Unit] = async {
    upsertedUserPrototypes += userPrototype
  }

  // **************** Additional methods for tests ****************//
  def addEntities[E <: Entity: EntityType](entities: E*): Unit = {
    modificationsBuffer.addEntities(entities.toVector)
  }

  def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()

  def allUpsertedUserPrototypes: Seq[UserPrototype] = upsertedUserPrototypes.toVector
}
