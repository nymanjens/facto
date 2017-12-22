package models.manager

import models.Entity
import models.access.{Fields, RemoteDatabaseProxy}
import models.modification.EntityType

import scala.async.Async.{async, await}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._
import models.access.Fields

abstract class BaseJsEntityManager[E <: Entity: EntityType](implicit database: RemoteDatabaseProxy)
    extends EntityManager[E] {

  // **************** Implementation of EntityManager ****************//
  override final def findById(id: Long) = async {
    await(database.newQuery().findOne(Fields.id, id)).get
  }

  override final def fetchAll() = {
    database.newQuery().data()
  }
}
