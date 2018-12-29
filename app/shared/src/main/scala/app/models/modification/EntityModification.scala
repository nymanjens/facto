package app.models.modification

import java.lang.Math.abs

import hydro.models.Entity

import scala.util.Random

/**
  * Indicates an idempotent addition, update or removal of an entity.
  *
  * This modification may be used for desired modifications (not yet persisted) or to indicate an already changed state.
  *
  * It is important that these modifications are created and treated as idempotent modifications, i.e. applying a
  * modification a second time is a no-op.
  */
sealed trait EntityModification {
  def entityType: EntityType.any
  def entityId: Long
}

object EntityModification {
  def createAddWithRandomId[E <: Entity: EntityType](entityWithoutId: E): Add[E] = {
    require(entityWithoutId.idOption.isEmpty, entityWithoutId)

    val entityWithId = Entity.withId(generateRandomId(), entityWithoutId)
    Add(entityWithId)
  }

  def createAddWithId[E <: Entity: EntityType](id: Long, entityWithoutId: E): Add[E] = {
    require(entityWithoutId.idOption.isEmpty, entityWithoutId)

    val entityWithId = Entity.withId(id, entityWithoutId)
    Add(entityWithId)
  }

  def createUpdate[E <: Entity: EntityType](entity: E): Update[E] = Update(entity)

  def createDelete[E <: Entity: EntityType](entityWithId: E): Remove[E] = {
    require(entityWithId.idOption.isDefined, entityWithId)

    Remove[E](entityWithId.id)
  }

  def generateRandomId(): Long = abs(Random.nextLong)

  case class Add[E <: Entity: EntityType](entity: E) extends EntityModification {
    require(entity.idOption.isDefined, s"Entity ID must be defined (for entity $entity)")
    entityType.checkRightType(entity)

    override def entityType: EntityType[E] = implicitly[EntityType[E]]
    override def entityId: Long = entity.id
  }

  /**
    * Update to an existing entity.
    *
    * Warning: Different clients could end up in different states if the ordering of Updates are changed. Don't use
    * this for client-created modifications. Use Delete + Add instead.
    */
  case class Update[E <: Entity: EntityType](updatedEntity: E) extends EntityModification {
    require(updatedEntity.idOption.isDefined, s"Entity ID must be defined (for entity $updatedEntity)")
    entityType.checkRightType(updatedEntity)

    override def entityType: EntityType[E] = implicitly[EntityType[E]]
    override def entityId: Long = updatedEntity.id
  }

  case class Remove[E <: Entity: EntityType](override val entityId: Long) extends EntityModification {
    override def entityType: EntityType[E] = implicitly[EntityType[E]]
  }
}
