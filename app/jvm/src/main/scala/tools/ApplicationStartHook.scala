package tools

import java.nio.file.{Files, Path, Paths}

import com.google.inject.Inject
import common.ResourceFiles
import models.accounting.money.{ExchangeRateMeasurement, ExchangeRateMeasurements}
import models._
import org.joda.time.DateTime
import play.api.{Application, Mode}

import scala.collection.JavaConverters._

final class ApplicationStartHook @Inject()(implicit app: Application,
                                           userManager: User.Manager,
                                           entityAccess: SlickEntityAccess,
                                           generalImportTool: GeneralImportTool,
                                           csvImportTool: CsvImportTool) {
  onStart()

  private def onStart(): Unit = {
    processFlags()

    // Set up database if necessary
    if (Set(Mode.Test, Mode.Dev) contains app.mode) {
      if (AppConfigHelper.dropAndCreateNewDb) {
        generalImportTool.dropAndCreateNewDb()
      }
    }

    // Initialize table managers (notably the caching ones)
    for (entityManager <- entityAccess.allEntityManagers) {
      entityManager.initialize()
    }

    // Populate the database with dummy data
    if (Set(Mode.Test, Mode.Dev) contains app.mode) {
      if (AppConfigHelper.loadDummyUsers) {
        loadDummyUsers()
      }
      if (AppConfigHelper.loadCsvDummyData) {
        loadCsvDummyData(AppConfigHelper.csvDummyDataFolder)
      }
    }
  }

  private def processFlags() = {
    if (CommandLineFlags.dropAndCreateNewDb()) {
      println("")
      println("  Dropping the database tables (if present) and creating new ones...")
      generalImportTool.dropAndCreateNewDb()
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
      userManager.add(userManager.newWithUnhashedPw(loginName, password, name = "Admin"))
      println("  Done. Exiting.")

      System.exit(0)
    }
  }

  private def loadDummyUsers() = {
    userManager.add(userManager.newWithUnhashedPw(loginName = "admin", password = "a", name = "Admin"))
    userManager.add(userManager.newWithUnhashedPw(loginName = "alice", password = "a", name = "Alice"))
    userManager.add(userManager.newWithUnhashedPw(loginName = "bob", password = "b", name = "Bob"))
  }

  private def loadCsvDummyData(csvDataFolder: Path) = {
    csvImportTool.importTransactions(assertExists(csvDataFolder resolve "transactions.csv"))
    csvImportTool.importBalanceChecks(assertExists(csvDataFolder resolve "balancechecks.csv"))
    ExchangeRateMeasurements.add(ExchangeRateMeasurement(
      date = new DateTime(1990, 1, 1, 0, 0), // Jan 1, 1990
      foreignCurrencyCode = "GBP",
      ratioReferenceToForeignCurrency = 1.2))
  }

  private def assertExists(path: Path): Path = {
    require(ResourceFiles.exists(path), s"Couldn't find path: $path")
    path
  }

  private object CommandLineFlags {
    private val properties = System.getProperties.asScala

    def dropAndCreateNewDb(): Boolean = getBoolean("dropAndCreateNewDb")
    def createAdminUser(): Boolean = getBoolean("createAdminUser")

    private def getBoolean(name: String): Boolean = properties.get(name).isDefined

    private def getExistingPath(name: String): Option[Path] =
      properties.get(name) map (Paths.get(_)) map assertExists
  }

  private object AppConfigHelper {
    def dropAndCreateNewDb: Boolean = getBoolean("facto.development.dropAndCreateNewDb")
    def loadDummyUsers: Boolean = getBoolean("facto.development.loadDummyUsers")
    def loadCsvDummyData: Boolean = getBoolean("facto.development.loadCsvDummyData")
    def csvDummyDataFolder: Path = getExistingPath("facto.development.csvDummyDataFolder")
    def defaultPassword: Option[String] = getString("facto.setup.defaultPassword")

    private def getBoolean(cfgPath: String): Boolean =
      app.configuration.getBoolean(cfgPath) getOrElse false

    private def getString(cfgPath: String): Option[String] =
      app.configuration.getString(cfgPath)

    private def getExistingPath(cfgPath: String): Path = assertExists {
      Paths.get(app.configuration.getString(cfgPath).get)
    }
  }
}
