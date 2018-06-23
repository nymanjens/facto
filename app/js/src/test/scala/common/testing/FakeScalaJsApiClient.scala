package common.testing

import api.ScalaJsApi.{GetAllEntitiesResponse, ModificationsWithToken, UpdateToken}
import api.ScalaJsApiClient
import models.Entity
import models.access.{DbQuery, DbQueryExecutor}
import models.modification.{EntityModification, EntityType}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class FakeScalaJsApiClient extends ScalaJsApiClient {

  private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()

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

  // **************** Additional methods for tests ****************//
  def addEntities[E <: Entity: EntityType](entities: E*): Unit = {
    modificationsBuffer.addEntities(entities.toVector)
  }

  def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()
}
