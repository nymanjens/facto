package models.manager

import scala.collection.immutable.Seq

import slick.lifted.{AbstractTable, TableQuery}

import models.SlickUtils.dbApi._

/** Provides access to persisted entries using the Slick API. */
trait SlickEntityManager[E <: Entity, T <: AbstractTable[E]] extends EntityManager[E] {

  // ********** Management methods ********** //
  /** Initializes this manager. This is called once at the start of the application. */
  def initialize(): Unit = {}
  /** Creates the persisted database table for this manager. */
  def createTable(): Unit
  def tableName: String

  // ********** Mutators ********** //
  /** Persists a new entity (without ID) and returns the same entity with its ID in the database. */
  def add(entity: E): E

  /**
    * Persists a new entity (with ID) and returns the given entity.
    *
    * @throws IllegalArgumentException if an entry with given ID already exists.
    */
  def addWithId(entity: E): E

  /** Persists an update to an existing entity and returns the given entity. */
  def update(entity: E): E

  /** Deletes an existing entity from the database. */
  def delete(entity: E): Unit

  // ********** Getters ********** //
  /**
    * Returns a new query that should be run by models.SlickUtils.dbRun. Don't run any mutating operations using these queries!
    *
    * Don't run any mutating operations using these queries!
    */
  def newQuery: TableQuery[T]
}

object SlickEntityManager {

  /**
    * Factory method for creating a database backed SlickEntityManager.
    *
    * @param cached if true, the manager is decorated with a caching layer that loads all data in memory.
    */
  def create[E <: Entity, T <: EntityTable[E]](cons: Tag => T,
                                               tableName: String,
                                               cached: Boolean = false
                                              ): SlickEntityManager[E, T] = {
    var result: SlickEntityManager[E, T] = new DatabaseBackedEntityManager[E, T](cons, tableName)
    if (cached) {
      result = new CachingEntityManager[E, T](result)
    }
    result = new InvalidatingEntityManager[E, T](result)
    result
  }
}
