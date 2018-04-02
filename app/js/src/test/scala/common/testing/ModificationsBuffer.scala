package common.testing

import java.time.Duration
import java.time.Month.JANUARY

import api.ScalaJsApi.UpdateToken
import common.time.JavaTimeImplicits._
import common.time.LocalDateTime
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala.collection.mutable

final class ModificationsBuffer {

  private val buffer: mutable.Buffer[ModificationWithToken] = mutable.Buffer()

  // **************** Getters ****************//
  def getModifications(updateToken: UpdateToken = ModificationsBuffer.startToken): Seq[EntityModification] =
    Seq({
      for (m <- buffer if m.updateToken >= updateToken) yield m.modification
    }: _*)

  def getAllEntitiesOfType[E <: Entity](implicit entityType: EntityType[E]): Seq[E] = {
    val entitiesMap = mutable.LinkedHashMap[Long, E]()
    for (m <- buffer if m.modification.entityType == entityType) {
      m.modification match {
        case EntityModification.Add(entity) => entitiesMap.put(entity.id, entityType.checkRightType(entity))
        case EntityModification.Update(entity) =>
          entitiesMap.put(entity.id, entityType.checkRightType(entity))
        case EntityModification.Remove(entityId) => entitiesMap.remove(entityId)
      }
    }
    entitiesMap.values.toVector
  }

  def nextUpdateToken: UpdateToken = {
    if (buffer.isEmpty) {
      ModificationsBuffer.startToken
    } else {
      buffer.map(_.updateToken).max plus Duration.ofDays(1)
    }
  }

  def isEmpty: Boolean = buffer.isEmpty

  // **************** Setters ****************//
  def addModifications(modifications: Seq[EntityModification]): Unit = {
    for (modification <- modifications) {
      buffer += ModificationWithToken(modification, nextUpdateToken)
    }
  }

  def addEntities[E <: Entity: EntityType](entities: Seq[E]): Unit = {
    addModifications(entities.map(e => EntityModification.Add(e)))
  }

  def clear(): Unit = {
    buffer.clear()
  }

  // **************** Inner types ****************//
  private case class ModificationWithToken(modification: EntityModification, updateToken: UpdateToken)
}

object ModificationsBuffer {
  private val startToken: UpdateToken = LocalDateTime.of(1990, JANUARY, 1, 0, 0)
}
