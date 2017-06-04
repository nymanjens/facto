package models.manager

import java.lang.Math.abs
import scala.util.Random

/**
  * Indicates an idempotent addition or removal of an immutable entity.
  *
  * This modification may used for desired modifications (not yet persisted) or to indicate an already changed state.
  *
  * It is important that these modifications are created and treated as idempotent modifications, i.e. applying a
  * modification a seconds time is a no-op.
  */
sealed trait EntityModification {
  def entityType: EntityType.any
  def entityId: Long
}

object EntityModification {
  def createAddWithRandomId[E <: Entity: EntityType](entityWithoutId: E): Add[E] = {
    require(entityWithoutId.idOption.isEmpty, entityWithoutId)

    val entityWithId = entityWithoutId.withId(generateRandomId()).asInstanceOf[E]
    Add(entityWithId)
  }

  def createAddWithId[E <: Entity: EntityType](entityWithoutId: E, id: Long): Add[E] = {
    require(entityWithoutId.idOption.isEmpty, entityWithoutId)

    val entityWithId = entityWithoutId.withId(id).asInstanceOf[E]
    Add(entityWithId)
  }

  def createDelete[E <: Entity: EntityType](entityWithId: E): Remove[E] = {
    require(entityWithId.idOption.isDefined, entityWithId)

    Remove[E](entityWithId.id)
  }

  def generateRandomId(): Long = abs(Random.nextLong)

  case class Add[E <: Entity: EntityType](entity: E) extends EntityModification {
    require(entity.idOption.isDefined)
    entityType.checkRightType(entity)

    override def entityType: EntityType[E] = implicitly[EntityType[E]]
    override def entityId: Long = entity.id
  }

  case class Remove[E <: Entity: EntityType](override val entityId: Long) extends EntityModification {
    override def entityType: EntityType[E] = implicitly[EntityType[E]]
  }
}
