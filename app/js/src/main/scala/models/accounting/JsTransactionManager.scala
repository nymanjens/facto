package models.accounting

import api.ScalaJsApi.EntityType
import models.access.{EntityModification, RemoteDatabaseProxy}
import models.manager.{BaseJsEntityManager, Entity, EntityManager}

import scala.collection.immutable.Seq
import scala.util.Random
import scala2js.Scala2Js
import scala2js.Converters
import scala.collection.immutable.Seq

private[accounting] final class JsTransactionManager(database: RemoteDatabaseProxy)
  extends BaseJsEntityManager[Transaction](database)
    with Transaction.Manager {

  override def findByGroupId(groupId: Long): Seq[Transaction] = ???

  protected final val entityType: EntityType = EntityType.TransactionType
}
