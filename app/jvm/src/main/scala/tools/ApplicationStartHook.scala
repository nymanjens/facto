package tools

import java.nio.file.{Path, Paths}
import java.time.Month.JANUARY
import java.time.{LocalDate, LocalTime}

import com.google.inject.Inject
import common.ResourceFiles
import common.time.{Clock, LocalDateTime}
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models._
import models.modification.EntityModification
import models.modificationhandler.EntityModificationHandler
import models.money.ExchangeRateMeasurement
import models.user.{SlickUserManager, User}
import play.api.{Application, Mode}

import scala.collection.JavaConverters._

final class ApplicationStartHook @Inject()(implicit app: Application,
                                           entityAccess: SlickEntityAccess,
                                           csvImportTool: CsvImportTool,
                                           entityModificationHandler: EntityModificationHandler,
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
      implicit val user = SlickUserManager.getOrCreateRobotUser()

      val loginName = "admin"
      val password = AppConfigHelper.defaultPassword getOrElse "changeme"

      println("")
      println("  Createing admin user...")
      println(s"    loginName: $loginName")
      println(s"    password: $password")
      entityModificationHandler.persistEntityModifications(
        EntityModification.createAddWithRandomId(
          SlickUserManager.createUser(loginName, password, name = "Admin")))
      println("  Done. Exiting.")

      System.exit(0)
    }
  }

  private def dropAndCreateNewDb(): Unit = {
    println("  Creating tables...")

    for (entityManager <- entityAccess.allEntityManagers) {
      dbRun(sqlu"""DROP TABLE IF EXISTS #${entityManager.tableName}""")
      entityManager.createTable()
    }

    println("   done")
  }

  private def loadDummyUsers(): Unit = {
    implicit val user = SlickUserManager.getOrCreateRobotUser()

    entityModificationHandler.persistEntityModifications(
      EntityModification.createAddWithRandomId(
        SlickUserManager.createUser(loginName = "admin", password = "a", name = "Admin")),
      EntityModification.createAddWithRandomId(
        SlickUserManager.createUser(loginName = "alice", password = "a", name = "Alice")),
      EntityModification.createAddWithRandomId(
        SlickUserManager.createUser(loginName = "bob", password = "b", name = "Bob"))
    )
  }

  private def loadCsvDummyData(csvDataFolder: Path): Unit = {
    implicit val user = SlickUserManager.getOrCreateRobotUser()

    csvImportTool.importTransactions(assertExists(csvDataFolder resolve "transactions.csv"))
    csvImportTool.importBalanceChecks(assertExists(csvDataFolder resolve "balancechecks.csv"))
    entityModificationHandler.persistEntityModifications(
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
    def dropAndCreateNewDb: Boolean = getBoolean("facto.development.dropAndCreateNewDb")
    def loadDummyUsers: Boolean = getBoolean("facto.development.loadDummyUsers")
    def loadCsvDummyData: Boolean = getBoolean("facto.development.loadCsvDummyData")
    def csvDummyDataFolder: Path = getExistingPath("facto.development.csvDummyDataFolder")
    def defaultPassword: Option[String] = getString("facto.setup.defaultPassword")

    private def getBoolean(cfgPath: String): Boolean =
      app.configuration.getOptional[Boolean](cfgPath) getOrElse false

    private def getString(cfgPath: String): Option[String] =
      app.configuration.getOptional[String](cfgPath)

    private def getExistingPath(cfgPath: String): Path = assertExists {
      Paths.get(app.configuration.get[String](cfgPath))
    }
  }
}
