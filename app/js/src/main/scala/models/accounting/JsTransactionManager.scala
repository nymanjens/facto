package models.accounting

import jsfacades.LokiJs
import models.access.DbQuery
import models.access.DbQueryImplicits._
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq
import scala2js.Converters._
import models.access.Fields

final class JsTransactionManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[Transaction]
    with Transaction.Manager {

  override def findByGroupId(groupId: Long) = {
    database
      .newQuery[Transaction]()
      .filter(Fields.Transaction.transactionGroupId isEqualTo groupId)
      .sort(DbQuery.Sorting.ascBy(Fields.id))
      .data()
  }
}
