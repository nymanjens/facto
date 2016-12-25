package models.accounting

import api.ScalaJsApi.EntityType
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Scala2Js

private[accounting] final class JsTransactionManager(database: RemoteDatabaseProxy)
  extends BaseJsEntityManager[Transaction](database)
    with Transaction.Manager {

  override def findByGroupId(groupId: Long): Seq[Transaction] = {
    val result = database.newQuery(entityType).find("transactionGroupId" -> groupId.toString).data()
    Scala2Js.toScala[Seq[Transaction]](result)
  }

  protected final val entityType: EntityType = EntityType.TransactionType
}
