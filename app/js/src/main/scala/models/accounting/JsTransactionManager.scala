package models.accounting

import models.access.DbQueryImplicits._
import models.access.{DbQuery, ModelField, RemoteDatabaseProxy}
import models.manager.BaseJsEntityManager

import scala2js.Converters._

final class JsTransactionManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[Transaction]
    with Transaction.Manager