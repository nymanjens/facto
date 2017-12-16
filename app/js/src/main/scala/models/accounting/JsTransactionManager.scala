package models.accounting

import jsfacades.LokiJs
import jsfacades.LokiJsImplicits._
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class JsTransactionManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[Transaction]
    with Transaction.Manager {

  override def findByGroupId(groupId: Long): Seq[Transaction] = {
    ???
//    database
//      .newQuery[Transaction]()
//      .filter(Keys.Transaction.transactionGroupId isEqualTo groupId)
//      .sort(LokiJs.Sorting.ascBy(Keys.id))
//      .data()
  }
}
