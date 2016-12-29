package models.manager

import scala.util.Random

/**
  * Indicates an addition or removal of an immutable entity.
  *
  * This modification may used for desired modifications (not yet persisted) or to indicate an already changed state.
  */
sealed trait EntityModification {
  def entityType: EntityType.any
  def entityId: Long
}

object EntityModification {
  def createAddWithRandomId[E <: Entity : EntityType](entityWithoutId: E): Add[E] = {
    require(entityWithoutId.idOption.isEmpty, entityWithoutId)

    val id = Random.nextLong
    val entityWithId = entityWithoutId.withId(id).asInstanceOf[E]
    Add(entityWithId)
  }

  def createDelete[E <: Entity : EntityType](entityWithId: E): Remove[E] = {
    require(entityWithId.idOption.isDefined, entityWithId)

    Remove[E](entityWithId.id)
  }

  case class Add[E <: Entity : EntityType](entity: E) extends EntityModification {
    require(entity.idOption.isDefined)
    entityType.checkRightType(entity)

    override def entityType: EntityType[E] = implicitly[EntityType[E]]
    override def entityId: Long = entity.id
  }

  case class Remove[E <: Entity : EntityType](override val entityId: Long) extends EntityModification {
    override def entityType: EntityType[E] = implicitly[EntityType[E]]
  }
}
