import java.nio.file.{Files, Paths, Path}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

import java.lang.System

import play.api.{Application, Mode, Logger, GlobalSettings}

import models.{Tables, Users}
import tools.GeneralImportTool.dropAndCreateNewDb
import tools.CsvImportTool
import tools.FactoV1ImportTool


object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    implicit val _ = app

    processFlags()

    // Set up database if necessary
    if (Set(Mode.Test, Mode.Dev) contains app.mode) {
      if (AppConfigHelper.dropAndCreateNewDb) {
        dropAndCreateNewDb()
      }
    }

    // Initialize table managers (notably the caching ones)
    for (tableManager <- Tables.allManagers) {
      tableManager.initialize()
    }

    // Populate the database with dummy data
    if (Set(Mode.Test, Mode.Dev) contains app.mode) {
      if (AppConfigHelper.loadDummyUsers) {
        loadDummyUsers()
      }
      if (AppConfigHelper.loadCsvDummyData) {
        loadCsvDummyData(AppConfigHelper.csvDummyDataFolder)
      } else if (AppConfigHelper.loadFactoV1Data) {
        FactoV1ImportTool.importFromSqlDump(AppConfigHelper.factoV1SqlFilePath)
      }
    }
  }

  private def processFlags()(implicit app: Application) = {
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

    if (CommandLineFlags.createAdminUser()) {
      val loginName = "admin"
      val password = AppConfigHelper.defaultPassword getOrElse "changeme"

      println("")
      println("  Createing admin user...")
      println(s"    loginName: $loginName")
      println(s"    password: $password")
      Users.add(Users.newWithUnhashedPw(loginName, password, name = "Admin"))
      println("  Done. Exiting.")

      System.exit(0)
    }
  }

  private def loadDummyUsers() = {
    Users.add(Users.newWithUnhashedPw(loginName = "admin", password = "a", name = "Admin"))
    Users.add(Users.newWithUnhashedPw(loginName = "alice", password = "a", name = "Alice"))
    Users.add(Users.newWithUnhashedPw(loginName = "bob", password = "b", name = "Bob"))
  }

  private def loadCsvDummyData(csvDataFolder: Path) = {
    CsvImportTool.importTransactions(assertExists(csvDataFolder resolve "transactions.csv"))
    CsvImportTool.importBalanceChecks(assertExists(csvDataFolder resolve "balancechecks.csv"))
  }

  private def assertExists(path: Path): Path = {
    require(Files.exists(path), s"Couldn't find path: $path")
    path
  }

  private object CommandLineFlags {
    private val properties = System.getProperties.asScala

    def loadFactoV1DataFromPath(): Option[Path] = getExistingPath("loadFactoV1DataFromPath")
    def dropAndCreateNewDb(): Boolean = getBoolean("dropAndCreateNewDb")
    def createAdminUser(): Boolean = getBoolean("createAdminUser")

    private def getBoolean(name: String): Boolean = properties.get(name).isDefined

    private def getExistingPath(name: String): Option[Path] =
      properties.get(name) map (Paths.get(_)) map assertExists
  }

  private object AppConfigHelper {
    def dropAndCreateNewDb(implicit app: Application): Boolean = getBoolean("facto.development.dropAndCreateNewDb")
    def loadDummyUsers(implicit app: Application): Boolean = getBoolean("facto.development.loadDummyUsers")
    def loadCsvDummyData(implicit app: Application): Boolean = getBoolean("facto.development.loadCsvDummyData")
    def csvDummyDataFolder(implicit app: Application): Path = getExistingPath("facto.development.csvDummyDataFolder")
    def loadFactoV1Data(implicit app: Application): Boolean = getBoolean("facto.development.loadFactoV1Data")
    def factoV1SqlFilePath(implicit app: Application): Path = getExistingPath("facto.development.factoV1SqlFilePath")
    def defaultPassword(implicit app: Application): Option[String] = getString("facto.setup.defaultPassword")

    private def getBoolean(cfgPath: String)(implicit app: Application): Boolean =
      app.configuration.getBoolean(cfgPath) getOrElse false

    private def getString(cfgPath: String)(implicit app: Application): Option[String] =
      app.configuration.getString(cfgPath)

    private def getExistingPath(cfgPath: String)(implicit app: Application): Path = assertExists {
      Paths.get(app.configuration.getString(cfgPath).get)
    }
  }
}
