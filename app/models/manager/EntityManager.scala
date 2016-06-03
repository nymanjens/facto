package models.manager

import scala.collection.immutable.Seq

/** Provides access to persisted entries. */
trait EntityManager[E <: Identifiable[E]] {

  // ********** Management methods ********** //
  /** Initializes this manager. This is called once at the start of the application. */
  def initialize(): Unit = {}
  /** Throws an exception if there is a consistency problem in this manager. */
  def verifyConsistency(): Unit = {}
  /** Creates the persisted database table for this manager. */
  def createTable: Unit
  def tableName: String

  // ********** Mutators ********** //
  /** Persists a new entity (without ID) and returns the same entity with its ID in the database. */
  def add(entity: E): E
  /** Persists an update to an existing entity and returns the given entity. */
  def update(entity: E): E
  /** Deletes an existing entity from the database. */
  def delete(entity: E): Unit

  // ********** Getters ********** //
  /** Returns the entity with given ID or throws an exception. */
  def findById(id: Long): E
  /** Returns the result of given function on the stream of all entities. Note that the result must be immutable. */
  def fetchFromAll[R](calculateResult: Stream[E] => R): R
  /** Returns the result of given function on the stream of all entities. */
  def fetchAll(selection: Stream[E] => Stream[E] = s => s): List[E] = fetchFromAll(stream => selection(stream).toList)
  /** Returns the number of entities that conform to given predicate. */
  def count(predicate: E => Boolean = _ => true): Int = fetchFromAll(_.count(predicate))
}

object EntityManager {
  /** Decorates the given manager with a caching layer that loads all data in memory. */
  def caching[E <: Identifiable[E]](delegate: EntityManager[E]): EntityManager[E] =
    new CachingEntityManager(delegate)
}
