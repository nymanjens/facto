package models

import models.accounting._
import models.accounting.money.ExchangeRateMeasurement

abstract class EntityAccess(implicit val userManager: User.Manager,
                   val balanceCheckManager: BalanceCheck.Manager,
                   val tagEntityManager: TagEntity.Manager,
                   val transactionManager: Transaction.Manager,
                   val transactionGroupManager: TransactionGroup.Manager,
                   val updateLogManager: UpdateLog.Manager,
                   val exchangeRateMeasurementManager: ExchangeRateMeasurement.Manager)
