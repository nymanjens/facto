package models.accounting

import jsfacades.Loki
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager
import scala2js.Converters._
import scala2js.Keys
import scala.collection.immutable.Seq

final class JsTransactionManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[Transaction]
    with Transaction.Manager {

  override def findByGroupId(groupId: Long): Seq[Transaction] = {
    database
      .newQuery[Transaction]()
      .filter(Keys.Transaction.transactionGroupId, groupId)
      .sort(Loki.Sorting.ascBy("id"))
      .data()
  }
}
