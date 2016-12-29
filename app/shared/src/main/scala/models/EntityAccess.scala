package models

import models.accounting._
import models.accounting.money.ExchangeRateMeasurement

abstract class EntityAccess(implicit val userManager: User.Manager,
                   val balanceCheckManager: BalanceCheck.Manager,
                   val transactionManager: Transaction.Manager,
                   val transactionGroupManager: TransactionGroup.Manager,
                   val exchangeRateMeasurementManager: ExchangeRateMeasurement.Manager)
