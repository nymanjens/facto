package models

import models.accounting._
import models.accounting.money.JsExchangeRateMeasurementManager

final class JsEntityAccess(implicit override val userManager: JsUserManager,
                           override val balanceCheckManager: JsBalanceCheckManager,
                           override val transactionManager: JsTransactionManager,
                           override val transactionGroupManager: JsTransactionGroupManager,
                           override val exchangeRateMeasurementManager: JsExchangeRateMeasurementManager
                          ) extends EntityAccess
