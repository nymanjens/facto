package models.accounting

import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

final class JsTransactionGroupManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[TransactionGroup]
    with TransactionGroup.Manager
