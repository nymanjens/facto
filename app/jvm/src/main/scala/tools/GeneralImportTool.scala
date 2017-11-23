package tools

import com.google.inject.Inject
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models._
import play.api.Logger

final class GeneralImportTool @Inject()(implicit userManager: User.Manager, entityAccess: SlickEntityAccess) {

  def dropAndCreateNewDb() = {
    Logger.debug("Creating tables...")

    for (entityManager <- entityAccess.allEntityManagers) {
      dbRun(sqlu"""DROP TABLE IF EXISTS #${entityManager.tableName}""")
      entityManager.createTable()
    }

    Logger.debug(" done")
  }
}
