package models.accounting

import models.access.DbQueryImplicits._
import models.access.{DbQuery, Fields, RemoteDatabaseProxy}
import models.manager.BaseJsEntityManager

import scala2js.Converters._

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
