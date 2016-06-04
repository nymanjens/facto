package models.manager

import scala.collection.immutable.Seq

import slick.lifted.{AbstractTable, TableQuery}

import models.SlickUtils.dbApi._

/** Provides access to persisted entries. */
trait EntityManager[E <: Identifiable[E], T <: AbstractTable[E]] {

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
  /** Returns all stored entities. */
  def fetchAll(): List[E]
  /**
    * Returns a new query that should be run by models.SlickUtils.dbRun. Don't run any mutating operations using these queries!
    *
    * Don't run any mutating operations using these queries!
    */
  def newQuery: TableQuery[T]
}

object EntityManager {
  /** Decorates the given manager with a caching layer that loads all data in memory. */
  //  def caching[E <: Identifiable[E]](delegate: EntityManager[E]): EntityManager[E] = ???

  /** Factory method for creating a database backed EntityManager */
  def create[E <: Identifiable[E], T <: EntityTable[E]](cons: Tag => T,
                                                        tableName: String,
                                                        cached: Boolean = false
                                                       ): EntityManager[E, T] = {
    var result: EntityManager[E, T] = new DatabaseBackedEntityManager[E, T](cons, tableName)
    if (cached) {
      result = new CachingEntityManager[E, T](result)
    }
    result = new InvalidatingEntityManager[E, T](result)
    result
  }
}
