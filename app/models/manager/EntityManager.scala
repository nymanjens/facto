package models.manager

import scala.collection.immutable.Seq

/** Provides access to persisted entries. */
trait EntityManager[E <: Identifiable[E]] {

  // ********** Management methods ********** //
  /* Initializes this manager. This is called once at the start of the application. */
  def initialize(): Unit = {}
  /* Throws an exception if there is a consistency problem in this manager. */
  def verifyConsistency(): Unit = {}
  /* Creates the persisted database table for this manager. */
  def createSchema: Unit
  def tableName: String

  // ********** Mutators ********** //
  def add(entity: E): E
  def update(entity: E): E
  def delete(entity: E): Unit

  // ********** Getters ********** //
  def findById(id: Long): E
  def fetchFromAll[R](calculateResult: Stream[E] => R): R
  def fetchAll(selection: Stream[E] => Stream[E] = s => s): List[E] = fetchFromAll(stream => selection(stream).toList)
  def count(predicate: E => Boolean = _ => true): Int = fetchFromAll(_.count(predicate))
}

object EntityManager {
  def caching[E <: Identifiable[E]](delegate: EntityManager[E]): EntityManager[E] =
    new CachingEntityManager(delegate)
}