package models.manager

/** Provides access to persisted entries. */
trait EntityManager[E <: Entity[E]] {

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
}
