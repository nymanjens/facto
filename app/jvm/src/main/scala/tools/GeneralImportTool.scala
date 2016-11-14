package tools

import play.api.Logger

import com.google.inject.Inject
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models._

final class GeneralImportTool @Inject()(implicit userManager: UserManager,
                                        entityAccess: SlickEntityAccess) {

  def dropAndCreateNewDb() = {
    Logger.debug("Creating tables...")

    for (entityManager <- entityAccess.allEntityManagers) {
      dbRun(
        sqlu"""DROP TABLE IF EXISTS #${
          entityManager.tableName
        }""")
      entityManager.createTable()
    }

    Logger.debug(" done")
  }
}
