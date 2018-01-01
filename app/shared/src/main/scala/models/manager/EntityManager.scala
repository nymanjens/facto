package models.manager

import models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Provides access to persisted entries. */
trait EntityManager[E <: Entity] {

  // ********** Getters ********** //
  /** Returns all stored entities. */
  def fetchAll(): Future[Seq[E]]
}
