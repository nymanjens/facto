package models.manager

import models.Entity
import models.access.{Fields, RemoteDatabaseProxy}
import models.modification.EntityType

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

abstract class BaseJsEntityManager[E <: Entity: EntityType](implicit database: RemoteDatabaseProxy)
    extends EntityManager[E] {

  // **************** Implementation of EntityManager ****************//
  override final def findById(id: Long): E = {
    database.newQuery().findOne(Fields.id, id).get
  }

  override final def fetchAll(): Seq[E] = {
    database.newQuery().data()
  }
}
