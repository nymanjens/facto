package models

import models.accounting.Transaction
import models.accounting.JsTransactionManager

object Module {

  import com.softwaremill.macwire._
  import models.access.Module._

  implicit lazy val transactionManager: Transaction.Manager = wire[JsTransactionManager]
}