package models.accounting

import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

final class JsBalanceCheckManager(implicit database: RemoteDatabaseProxy)
  extends BaseJsEntityManager[BalanceCheck]
    with BalanceCheck.Manager
