package models.manager

import models.Entity
import slick.lifted.AbstractTable

/**
  * SlickEntityManager base implementation that forwards all calls to a given delegate except for update(), which throws
  * an UnsupportedOperationException.
  */
abstract class ImmutableEntityManager[E <: Entity, T <: AbstractTable[E]](delegate: SlickEntityManager[E, T])
    extends ForwardingEntityManager[E, T](delegate) {

  // ********** Mutators ********** //
  override final def update(entity: E): E = throw new UnsupportedOperationException()
}
