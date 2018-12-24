package tools

import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month.JANUARY

import com.google.inject.Inject
import common.ResourceFiles
import common.time.Clock
import common.time.LocalDateTime
import models.access.JvmEntityAccess
import models.modification.EntityModification
import models.money.ExchangeRateMeasurement
import models.user.Users
import play.api.Application
import play.api.Mode

import scala.collection.JavaConverters._

final class ApplicationStartHook @Inject()(implicit app: Application,
                                           entityAccess: JvmEntityAccess,
                                           csvImportTool: CsvImportTool,
                                           clock: Clock) {
  onStart()

  private def onStart(): Unit = {
    processFlags()

    // Set up database if necessary
    if (app.mode == Mode.Test || app.mode == Mode.Dev) {
      if (AppConfigHelper.dropAndCreateNewDb) {
        dropAndCreateNewDb()
      }
    }

    // Populate the database with dummy data
    if (app.mode == Mode.Test || app.mode == Mode.Dev) {
      if (AppConfigHelper.loadDummyUsers) {
        loadDummyUsers()
      }
      if (AppConfigHelper.loadCsvDummyData) {
        loadCsvDummyData(AppConfigHelper.csvDummyDataFolder)
      }
    }
  }

  private def processFlags(): Unit = {
    if (CommandLineFlags.dropAndCreateNewDb) {
      println("")
      println("  Dropping the database tables (if present) and creating new ones...")
      dropAndCreateNewDb()
      println("  Done. Exiting.")

      System.exit(0)
    }

    if (CommandLineFlags.createAdminUser) {
      implicit val robotUser = Users.getOrCreateRobotUser()

      val loginName = "admin"
      val password = AppConfigHelper.defaultPassword getOrElse "changeme"

      println("")
      println(s"  Creating admin user...")
      println(s"      loginName: $loginName")
      println(s"      password: $password")
      entityAccess.persistEntityModifications(
        EntityModification.createAddWithRandomId(
          Users.createUser(loginName, password, name = "Admin", isAdmin = true)))
      println("  Done. Exiting.")

      System.exit(0)
    }
  }

  private def dropAndCreateNewDb(): Unit = {
    entityAccess.dropAndCreateTables()
  }

  private def loadDummyUsers(): Unit = {
    implicit val user = Users.getOrCreateRobotUser()

    entityAccess.persistEntityModifications(
      EntityModification.createAddWithRandomId(
        Users.createUser(loginName = "admin", password = "a", name = "Admin")),
      EntityModification.createAddWithRandomId(
        Users.createUser(loginName = "alice", password = "a", name = "Alice")),
      EntityModification.createAddWithRandomId(
        Users.createUser(loginName = "bob", password = "b", name = "Bob"))
    )
  }

  private def loadCsvDummyData(csvDataFolder: Path): Unit = {
    implicit val user = Users.getOrCreateRobotUser()

    csvImportTool.importTransactions(assertExists(csvDataFolder resolve "transactions.csv"))
    csvImportTool.importBalanceChecks(assertExists(csvDataFolder resolve "balancechecks.csv"))
    entityAccess.persistEntityModifications(
      EntityModification.createAddWithRandomId(
        ExchangeRateMeasurement(
          date = LocalDateTime.of(LocalDate.of(1990, JANUARY, 1), LocalTime.MIN),
          foreignCurrencyCode = "GBP",
          ratioReferenceToForeignCurrency = 1.2)))
  }

  private def assertExists(path: Path): Path = {
    require(ResourceFiles.exists(path), s"Couldn't find path: $path")
    path
  }

  private object CommandLineFlags {
    private val properties = System.getProperties.asScala

    def dropAndCreateNewDb: Boolean = getBoolean("dropAndCreateNewDb")
    def createAdminUser: Boolean = getBoolean("createAdminUser")

    private def getBoolean(name: String): Boolean = properties.get(name).isDefined

    private def getExistingPath(name: String): Option[Path] =
      properties.get(name) map (Paths.get(_)) map assertExists
  }

  private object AppConfigHelper {
    def dropAndCreateNewDb: Boolean = getBoolean("app.development.dropAndCreateNewDb")
    def loadDummyUsers: Boolean = getBoolean("app.development.loadDummyUsers")
    def loadCsvDummyData: Boolean = getBoolean("app.development.loadCsvDummyData")
    def csvDummyDataFolder: Path = getExistingPath("app.development.csvDummyDataFolder")
    def defaultPassword: Option[String] = getString("app.setup.defaultPassword")

    private def getBoolean(cfgPath: String): Boolean =
      app.configuration.getOptional[Boolean](cfgPath) getOrElse false

    private def getString(cfgPath: String): Option[String] =
      app.configuration.getOptional[String](cfgPath)

    private def getExistingPath(cfgPath: String): Path = assertExists {
      Paths.get(app.configuration.get[String](cfgPath))
    }
  }
}
