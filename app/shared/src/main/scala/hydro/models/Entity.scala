package hydro.models

// Based on active-slick (https://github.com/strongtyped/active-slick)

/** Base trait to define a model having an ID (i.e.: Entity). */
trait Entity {

  /** Returns the Entity ID */
  final def id: Long = idOption.getOrElse(throw new IllegalStateException(s"This entity has no ID: $this"))

  /**
    * The Entity ID wrapped in an Option.
    * Expected to be None when Entity not yet persisted, otherwise Some[Id].
    */
  def idOption: Option[Long]

  /** Returns a copy of this Entity with an ID. */
  def withId(id: Long): Entity
}

object Entity {
  def asEntity(entity: Entity): Entity = entity

  def withId[E <: Entity](id: Long, entity: E): E = entity.withId(id).asInstanceOf[E]
}
