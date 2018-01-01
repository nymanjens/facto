package api

import com.google.inject._
import common.GuavaReplacement.Iterables.getOnlyElement
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import models._
import models.accounting.SlickTransactionManager
import models.accounting.config._
import models.modification.{
  EntityModification,
  EntityModificationEntity,
  EntityType,
  SlickEntityModificationEntityManager
}
import models.user.Users
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ScalaJsApiServerFactoryTest extends HookedSpecification {

  private val date1 = localDateTimeOfEpochMilli(999000111)
  private val date2 = localDateTimeOfEpochMilli(999000222)
  private val date3 = localDateTimeOfEpochMilli(999000333)
  private val date4 = localDateTimeOfEpochMilli(999000444)

  implicit private val user = testUser

  @Inject implicit private val fakeClock: FakeClock = null
  @Inject implicit private val entityAccess: SlickEntityAccess = null
  @Inject implicit private val accountingConfig: Config = null
  @Inject private val userManager: SlickUserManager = null
  @Inject private val transactionManager: SlickTransactionManager = null
  @Inject private val modificationEntityManager: SlickEntityModificationEntityManager = null

  @Inject private val serverFactory: ScalaJsApiServerFactory = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "getInitialData()" in new WithApplication {
    val response = serverFactory.create().getInitialData()
    response.accountingConfig mustEqual accountingConfig
    response.user mustEqual user
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

    modificationEntityManager.fetchAll() must haveSize(1)
    val modificationEntity = getOnlyElement(modificationEntityManager.fetchAll())
    modificationEntity.userId mustEqual user.id
    modificationEntity.modification mustEqual testModification
    modificationEntity.date mustEqual testDate
  }
}
