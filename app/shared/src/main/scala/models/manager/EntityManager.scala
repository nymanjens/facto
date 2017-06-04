package models.manager

import scala.collection.immutable.Seq

/** Provides access to persisted entries. */
trait EntityManager[E <: Entity] {

  // ********** Getters ********** //
  /** Returns the entity with given ID or throws an exception. */
  def findById(id: Long): E

  /** Returns all stored entities. */
  def fetchAll(): Seq[E]
}
