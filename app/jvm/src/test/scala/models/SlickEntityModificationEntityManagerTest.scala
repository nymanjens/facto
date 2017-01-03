package models

import com.google.inject._
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import common.time.Clock
import models.accounting.BalanceCheck
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class SlickEntityModificationEntityManagerTest extends HookedSpecification {

  @Inject implicit private val clock: Clock = null
  @Inject implicit private val entityAccess: SlickEntityAccess = null
  @Inject private val userManager: SlickUserManager = null

  @Inject private val modificationEntityManager: SlickEntityModificationEntityManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the EntityModificationEntity model" in new WithApplication {
    userManager.addWithId(testUser)
    val modificationEntity = modificationEntityManager.add(EntityModificationEntity(
      userId = testUser.id,
      modification = testModification,
      date = testDate
    ))

    modificationEntity.user mustEqual testUser
    modificationEntityManager.fetchAll() mustEqual Seq(modificationEntity)
  }
}
