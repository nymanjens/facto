package api

import models.access.DbQueryImplicits._
import com.google.inject._
import common.GuavaReplacement.Iterables.getOnlyElement
import common.money.Currency
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import models.access.{DbQuery, JvmEntityAccess, ModelField}
import models.accounting.Transaction
import models.accounting.config._
import models.modification.{EntityModification, EntityModificationEntity, EntityType}
import models.money.ExchangeRateMeasurement
import models.slick.SlickUtils.dbRun
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.SortedMap
import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ScalaJsApiServerFactoryTest extends HookedSpecification {

  private val date1 = localDateTimeOfEpochMilli(999000111)
  private val date2 = localDateTimeOfEpochMilli(999000222)
  private val date3 = localDateTimeOfEpochMilli(999000333)
  private val date4 = localDateTimeOfEpochMilli(999000444)

  implicit private val user = testUserA

  @Inject implicit private val fakeClock: FakeClock = null
  @Inject implicit private val entityAccess: JvmEntityAccess = null
  @Inject implicit private val accountingConfig: Config = null

  @Inject private val serverFactory: ScalaJsApiServerFactory = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
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
    fakeClock.setTime(testDate)
    TestUtils.persist(testTransactionWithId)

    val response = serverFactory.create().getAllEntities(Seq(EntityType.TransactionType))

    response.entities(EntityType.TransactionType) mustEqual Seq(testTransactionWithId)
    response.nextUpdateToken mustEqual testDate
  }

  "getEntityModifications()" in new WithApplication {
    fakeClock.setTime(date1)
    entityAccess.persistEntityModifications(testModificationA)
    fakeClock.setTime(date3)
    entityAccess.persistEntityModifications(testModificationB)
    fakeClock.setTime(date4)

    val response = serverFactory.create().getEntityModifications(updateToken = date2)

    response.modifications mustEqual Seq(testModificationB)
    response.nextUpdateToken mustEqual date4
  }

  "persistEntityModifications()" in new WithApplication {
    fakeClock.setTime(testDate)

    serverFactory.create().persistEntityModifications(Seq(testModification))

    val modificationEntity = getOnlyElement(dbRun(entityAccess.newSlickQuery[EntityModificationEntity]()))
    modificationEntity.userId mustEqual user.id
    modificationEntity.modification mustEqual testModification
    modificationEntity.date mustEqual testDate
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
            filter = ModelField.Transaction.categoryCode === testCategoryA.code,
            sorting = None,
            limit = None)))

    entities.toSet mustEqual Set(transaction1, transaction2)
  }
}
