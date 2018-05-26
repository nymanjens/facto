package models.access

import common.GuavaReplacement.ImmutableSetMultimap
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala2js.Converters._

case class PendingModifications(private val modifications: Set[EntityModification],
                                persistedLocally: Boolean) {
  private val addModificationIds: ImmutableSetMultimap[EntityType.any, Long] = {
    val builder = ImmutableSetMultimap.builder[EntityType.any, Long]()
    modifications collect {
      case modification: EntityModification.Add[_] =>
        builder.put(modification.entityType, modification.entityId)
    }
    builder.build()
  }

  def additionIsPending[E <: Entity: EntityType](entity: E): Boolean = {
    addModificationIds.get(implicitly[EntityType[E]]) contains entity.id
  }

  def ++(otherModifications: Iterable[EntityModification]): PendingModifications =
    copy(modifications = modifications ++ otherModifications)

  def --(otherModifications: Iterable[EntityModification]): PendingModifications =
    copy(modifications = modifications -- otherModifications)
}
