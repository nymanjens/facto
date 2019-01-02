package app.api

import app.api.ScalaJsApi.UserPrototype
import app.api.UpdateTokens.toUpdateToken
import com.google.inject._
import app.common.GuavaReplacement.Iterables.getOnlyElement
import app.common.money.Currency
import app.common.testing.TestObjects._
import app.common.testing.TestUtils._
import app.common.testing._
import hydro.models.access.DbQueryImplicits._

import hydro.models.access.DbQuery
import app.models.access.AppDbQuerySorting
import app.models.access.AppDbQuerySorting
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import hydro.models.access.ModelField
import app.models.accounting.Transaction
import app.models.accounting.config._
import app.models.modification.EntityModification
import app.models.modification.EntityModificationEntity
import app.models.modification.EntityType
import app.models.modification.EntityTypes
import app.models.money.ExchangeRateMeasurement
import app.models.accounting.TransactionGroup
import app.models.accounting.Transaction
import app.models.accounting.BalanceCheck
import app.models.user.User
import app.models.money.ExchangeRateMeasurement
import hydro.models.access.DbQuery
import app.models.access.AppDbQuerySorting
import app.models.access.AppDbQuerySorting
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import hydro.models.access.ModelField
import app.models.modification.EntityModification
import app.models.modification.EntityModificationEntity
import app.models.modification.EntityType
import app.models.modification.EntityTypes
import app.models.money.ExchangeRateMeasurement
import app.models.accounting.TransactionGroup
import app.models.accounting.Transaction
import app.models.accounting.BalanceCheck
import app.models.user.User
import app.models.slick.SlickUtils.dbRun
import app.models.user.User
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.SortedMap
import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ScalaJsApiServerFactoryTest extends HookedSpecification {

  private val date1 = localDateTimeOfEpochSecond(999000111)
  private val date2 = localDateTimeOfEpochSecond(999000222)
  private val date3 = localDateTimeOfEpochSecond(999000333)
  private val date4 = localDateTimeOfEpochSecond(999000444)

  implicit private val user = testUserA

  @Inject implicit private val fakeClock: FakeClock = null
  @Inject implicit private val entityAccess: JvmEntityAccess = null
  @Inject implicit private val accountingConfig: Config = null

  @Inject private val serverFactory: ScalaJsApiServerFactory = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "getInitialData()" in new WithApplication {
    entityAccess.persistEntityModifications(
      EntityModification.Add(testUserA),
      EntityModification.Add(testUserB),
      EntityModification.createAddWithRandomId(
        ExchangeRateMeasurement(date1, "GBP", ratioReferenceToForeignCurrency = 1.3))
    )

    val response = serverFactory.create().getInitialData()

    response.accountingConfig mustEqual accountingConfig
    response.user mustEqual user
    response.allUsers.toSet mustEqual Set(testUserA, testUserB)
    response.ratioReferenceToForeignCurrency mustEqual Map(Currency.Gbp -> SortedMap(date1 -> 1.3))
  }

  "getAllEntities()" in new WithApplication {
    fakeClock.setNowInstant(testInstant)
    TestUtils.persist(testTransactionWithId)

    val response = serverFactory.create().getAllEntities(Seq(Transaction.Type))

    response.entities(Transaction.Type) mustEqual Seq(testTransactionWithId)
    response.nextUpdateToken mustEqual toUpdateToken(testInstant)
  }

  "persistEntityModifications()" in new WithApplication {
    fakeClock.setNowInstant(testInstant)

    serverFactory.create().persistEntityModifications(Seq(testModification))

    Awaiter.expectEventually.nonEmpty(dbRun(entityAccess.newSlickQuery[EntityModificationEntity]()))

    val modificationEntity = getOnlyElement(dbRun(entityAccess.newSlickQuery[EntityModificationEntity]()))
    modificationEntity.userId mustEqual user.id
    modificationEntity.modification mustEqual testModification
    modificationEntity.instant mustEqual testInstant
  }

  "executeDataQuery()" in new WithApplication {
    val transaction1 = persistTransaction(category = testCategoryA)
    val transaction2 = persistTransaction(category = testCategoryA)
    persistTransaction(category = testCategoryB)

    val entities = serverFactory
      .create()
      .executeDataQuery(
        PicklableDbQuery.fromRegular(
          DbQuery[Transaction](
            filter = ModelFields.Transaction.categoryCode === testCategoryA.code,
            sorting = None,
            limit = None)))

    entities.toSet mustEqual Set(transaction1, transaction2)
  }

  "upsertUser()" should {
    "add" in new WithApplication {
      serverFactory
        .create()(testUser.copy(isAdmin = true))
        .upsertUser(UserPrototype.create(loginName = "tester", plainTextPassword = "abc", name = "Tester"))

      val storedUser = getOnlyElement(entityAccess.newQuerySync[User]().data())

      storedUser.loginName mustEqual "tester"
      storedUser.name mustEqual "Tester"
      storedUser.isAdmin mustEqual false
      storedUser.expandCashFlowTablesByDefault mustEqual true
      storedUser.expandLiquidationTablesByDefault mustEqual true
    }

    "update" should {
      "password" in new WithApplication {
        serverFactory
          .create()(testUser.copy(isAdmin = true))
          .upsertUser(UserPrototype.create(loginName = "tester", plainTextPassword = "abc", name = "Tester"))
        val createdUser = getOnlyElement(entityAccess.newQuerySync[User]().data())
        serverFactory
          .create()(testUser.copy(idOption = Some(createdUser.id)))
          .upsertUser(UserPrototype.create(id = createdUser.id, plainTextPassword = "def"))
        val updatedUser = getOnlyElement(entityAccess.newQuerySync[User]().data())

        updatedUser.passwordHash mustNotEqual createdUser.passwordHash
      }
      "isAdmin" in new WithApplication {
        serverFactory
          .create()(testUser.copy(isAdmin = true))
          .upsertUser(UserPrototype
            .create(loginName = "tester", plainTextPassword = "abc", name = "Tester", isAdmin = false))
        val createdUser = getOnlyElement(entityAccess.newQuerySync[User]().data())
        serverFactory
          .create()(testUser.copy(idOption = Some(createdUser.id)))
          .upsertUser(UserPrototype.create(id = createdUser.id, isAdmin = true))
        val updatedUser = getOnlyElement(entityAccess.newQuerySync[User]().data())

        createdUser.isAdmin mustEqual false
        updatedUser.isAdmin mustEqual true
      }
    }
  }
}
