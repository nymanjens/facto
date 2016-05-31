package models.manager

// Based on active-slick (https://github.com/strongtyped/active-slick)

/** Base trait to define a model having an ID (i.e.: Entity). */
trait Identifiable[E <: Identifiable[E]] {

  /**
    * The Entity ID wrapped in an Option.
    * Expected to be None when Entity not yet persisted, otherwise Some[Id].
    */
  def id: Option[Long]

  /** Returns a copy of this Entity with an ID. */
  private[manager] def withId(id: Long): E
}
