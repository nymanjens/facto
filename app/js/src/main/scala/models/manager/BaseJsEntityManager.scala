package models.manager

import models.access.RemoteDatabaseProxy

import scala2js.Converters._
import scala.collection.immutable.Seq
import scala2js.Keys

abstract class BaseJsEntityManager[E <: Entity: EntityType](implicit database: RemoteDatabaseProxy)
    extends EntityManager[E] {

  // **************** Implementation of EntityManager ****************//
  override final def findById(id: Long): E = {
    database.newQuery().findOne(Keys.id, id).get
  }

  override final def fetchAll(): Seq[E] = {
    database.newQuery().data()
  }
}
