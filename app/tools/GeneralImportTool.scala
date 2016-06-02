package tools

import play.api.Logger

import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.Users
import models.accounting.{UpdateLogs, BalanceChecks, TransactionGroups, Transactions}

object GeneralImportTool {

  def dropAndCreateNewDb() = {
    Logger.debug("Creating tables...")

    dbRun(sqlu"""DROP TABLE IF EXISTS USERS""")
    Users.createSchema

    dbRun(sqlu"""DROP TABLE IF EXISTS TRANSACTIONS""")
    Transactions.createSchema

    dbRun(sqlu"""DROP TABLE IF EXISTS TRANSACTION_GROUPS""")
    TransactionGroups.createSchema

    dbRun(sqlu"""DROP TABLE IF EXISTS BALANCE_CHECKS""")
    BalanceChecks.createSchema

    dbRun(sqlu"""DROP TABLE IF EXISTS UPDATE_LOGS""")
    UpdateLogs.createSchema

    Logger.debug(" done")
  }
}
