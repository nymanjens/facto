import java.nio.file.{Files, Paths, Path}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

import java.lang.System

import play.api.{Application, Mode, Logger, GlobalSettings}

import models.Users
import tools.GeneralImportTool.dropAndCreateNewDb
import tools.CsvImportTool
import tools.FactoV1ImportTool


object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    implicit val _ = app

    if (CommandLineFlags.dropAndCreateNewDb()) {
      println("")
      println("  Dropping the database tables (if present) and creating new ones...")
      dropAndCreateNewDb()
      println("  Done. Exiting.")

      System.exit(0)
    }

    if (CommandLineFlags.loadFactoV1DataFromPath().isDefined) {
      val Some(factoV1SqlFilePath) = CommandLineFlags.loadFactoV1DataFromPath()

      println("")
      println(s"  Importing from facto v1 backup file (at $factoV1SqlFilePath)...")
      FactoV1ImportTool.importFromSqlDump(factoV1SqlFilePath)
      println("  Done. Exiting.")

      System.exit(0)
    }

    app.mode match {
      case Mode.Dev =>
        dropAndCreateNewDb()
        if (AppConfigHelper.loadCsvTestData) {
          populateDbWithCsvTestData(AppConfigHelper.csvTestDataFolder)
        } else if (AppConfigHelper.loadFactoV1Data) {
          FactoV1ImportTool.importFromSqlDump(AppConfigHelper.factoV1SqlFilePath)
        }

      case Mode.Test =>
        dropAndCreateNewDb()
    }
  }

  private def populateDbWithCsvTestData(csvDataFolder: Path) = {
    Logger.debug("Populating tables...")

    // populate users
    Users.all.save(Users.newWithUnhashedPw(loginName = "admin", password = "a", name = "Admin"))
    Users.all.save(Users.newWithUnhashedPw(loginName = "alice", password = "a", name = "Alice"))
    Users.all.save(Users.newWithUnhashedPw(loginName = "bob", password = "b", name = "Bob"))

    // populate transactions
    CsvImportTool.importTransactions(assertExists(csvDataFolder resolve "transactions.csv"))

    // populate BalanceChecks
    CsvImportTool.importBalanceChecks(assertExists(csvDataFolder resolve "balancechecks.csv"))

    Logger.debug(" done")
  }

  private def assertExists(path: Path): Path = {
    require(Files.exists(path), s"Couldn't find path: $path")
    path
  }

  private object CommandLineFlags {
    private val properties = System.getProperties.asScala

    def loadFactoV1DataFromPath(): Option[Path] = {
      properties.get("loadFactoV1DataFromPath").map(Paths.get(_))
    }

    def dropAndCreateNewDb(): Boolean = {
      properties.get("dropAndCreateNewDb").isDefined
    }
  }

  private object AppConfigHelper {
    def loadCsvTestData(implicit app: Application): Boolean =
      app.configuration.getBoolean("facto.development.loadCsvTestData") getOrElse false

    def csvTestDataFolder(implicit app: Application): Path = assertExists {
      Paths.get(app.configuration.getString("facto.development.csvTestDataFolder").get)
    }

    def loadFactoV1Data(implicit app: Application): Boolean =
      app.configuration.getBoolean("facto.development.loadFactoV1Data") getOrElse false

    def factoV1SqlFilePath(implicit app: Application): Path = assertExists {
      Paths.get(app.configuration.getString("facto.development.factoV1SqlFilePath").get)
    }
  }
}
