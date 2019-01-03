package hydro.common.testing

import app.api.ScalaJsApi.UpdateToken
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.collection.mutable

final class ModificationsBuffer {

  private val buffer: mutable.Buffer[ModificationWithToken] = mutable.Buffer()

  // **************** Getters ****************//
  def getModifications(updateToken: UpdateToken = ModificationsBuffer.startToken): Seq[EntityModification] =
    Seq({
      for (m <- buffer if m.updateToken.toLong >= updateToken.toLong) yield m.modification
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
      (buffer.map(_.updateToken.toLong).max + 1).toString
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
  private val startToken: UpdateToken = "0"
}
