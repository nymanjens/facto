package tools

import play.api.Logger

import slick.driver.H2Driver.api._

import models.ModelUtils.dbRun
import models.Users
import models.accounting.{UpdateLogs, BalanceChecks, TransactionGroups, Transactions}

object GeneralImportTool {

  def dropAndCreateNewDb() = {
    Logger.debug("Creating tables...")

    dbRun( sqlu"""DROP TABLE IF EXISTS USERS """)
    dbRun(Users.all.schema.create)

    dbRun( sqlu"""DROP TABLE IF EXISTS TRANSACTIONS """)
    dbRun(Transactions.all.schema.create)

    dbRun( sqlu"""DROP TABLE IF EXISTS TRANSACTION_GROUPS """)
    dbRun(TransactionGroups.all.schema.create)

    dbRun( sqlu"""DROP TABLE IF EXISTS BALANCE_CHECKS """)
    dbRun(BalanceChecks.all.schema.create)

    dbRun( sqlu"""DROP TABLE IF EXISTS UPDATE_LOGS """)
    dbRun(UpdateLogs.all.schema.create)

    Logger.debug(" done")
  }
}
