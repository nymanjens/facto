package models.manager

import slick.lifted.{AbstractTable, TableQuery}

/**
  * EntityManager base implementation that forwards all calls to a given delegate except for update(), which throws
  * an UnsupportedOperationException.
  */
abstract class ImmutableEntityManager[E <: Entity[E], T <: AbstractTable[E]](delegate: EntityManager[E, T])
  extends ForwardingEntityManager[E, T](delegate) {

  // ********** Mutators ********** //
  override final def update(entity: E): E = throw new UnsupportedOperationException()
}
