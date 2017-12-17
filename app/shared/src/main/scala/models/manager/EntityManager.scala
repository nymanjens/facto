package models.manager

import models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Provides access to persisted entries. */
trait EntityManager[E <: Entity] {

  // ********** Getters ********** //
  /** Returns the entity with given ID or throws an exception. */
  def findById(id: Long): Future[E]

  /** Returns all stored entities. */
  def fetchAll(): Future[Seq[E]]
}
