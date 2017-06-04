package models.manager

import models.access.RemoteDatabaseProxy

import scala.collection.immutable.Seq

abstract class BaseJsEntityManager[E <: Entity: EntityType](implicit database: RemoteDatabaseProxy)
    extends EntityManager[E] {

  // **************** Implementation of EntityManager ****************//
  override final def findById(id: Long): E = {
    database.newQuery().findOne("id" -> id.toString).get
  }

  override final def fetchAll(): Seq[E] = {
    database.newQuery().data()
  }
}
