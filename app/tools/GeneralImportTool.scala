package tools

import play.api.Logger

import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.Tables

object GeneralImportTool {

  def dropAndCreateNewDb() = {
    Logger.debug("Creating tables...")

    for (entityManager <- Tables.allEntityManagers) {
      dbRun(sqlu"""DROP TABLE IF EXISTS #${entityManager.tableName}""")
      entityManager.createTable
    }

    Logger.debug(" done")
  }
}
