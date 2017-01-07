package models.access

import java.time.Duration
import java.time.Month.MARCH

import api.ScalaJsApi.{GetAllEntitiesResponse, GetEntityModificationsResponse, UpdateToken}
import api.ScalaJsApiClient
import common.time.JavaTimeImplicits._
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.{Entity, EntityModification, EntityType}
import utest._
import common.testing.TestObjects._
import models.User

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js
import scala2js.Converters._


final class FakeScalaJsApiClient extends ScalaJsApiClient {

  private var modificationsBuffer: mutable.Buffer[ModificationWithToken] = mutable.Buffer()

  // **************** Implementation of ScalaJsApiClient trait ****************//
  override def getInitialData() = {
    ???
  }

  override def getAllEntities(types: Seq[EntityType.any]) = Future.successful {
    GetAllEntitiesResponse(
      entitiesMap = {
        for (entityType <- types) yield {
          entityType -> {
            var entitiesMap = mutable.Map[Long, Entity]()
            for (m <- modificationsBuffer if m.modification.entityType == entityType) {
              m.modification match {
                case EntityModification.Add(entity) => entitiesMap.put(entity.id, entity)
                case EntityModification.Remove(entityId) => entitiesMap.remove(entityId)
              }
            }
            entitiesMap.values.toVector
          }
        }
      }.toMap,
      nextUpdateToken = nextUpdateToken)
  }

  override def getEntityModifications(updateToken: UpdateToken) = Future.successful {
    GetEntityModificationsResponse(
      modifications = {
        for (m <- modificationsBuffer if m.updateToken >= updateToken) yield m.modification
      }.toVector,
      nextUpdateToken = nextUpdateToken)
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) = {
    for (modification <- modifications) {
      modificationsBuffer += ModificationWithToken(modification, nextUpdateToken)
    }
    Future.successful((): Unit)
  }

  // **************** Additional methods for setting data ****************//
  def addEntities[E <: Entity : EntityType](entities: Entity*): Unit = {
    persistEntityModifications(entities.map(EntityModification.Add[E](_)).toVector)
  }

  // **************** Private helper methods ****************//
  private def nextUpdateToken: UpdateToken = {
    modificationsBuffer.map(_.updateToken).max plus Duration.ofDays(1)
  }

  // **************** Inner types ****************//
  private case class ModificationWithToken(modification: EntityModification, updateToken: UpdateToken)
}
