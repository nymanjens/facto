package models.accounting

import jsfacades.Loki
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq

final class JsTransactionManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[Transaction]
    with Transaction.Manager {

  override def findByGroupId(groupId: Long): Seq[Transaction] = {
    database
      .newQuery[Transaction]()
      .find("transactionGroupId" -> groupId.toString)
      .sort(Loki.Sorting.ascBy("id"))
      .data()
  }
}
