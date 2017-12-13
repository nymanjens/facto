package models.modification

import com.google.inject._
import common.testing.TestObjects._
import common.testing._
import common.time.Clock
import models.SlickEntityAccess
import models.user.SlickUserManager
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
    userManager.addIfNew(testUser)

    val modificationEntity =
      EntityModificationEntity(
        idOption = Some(EntityModification.generateRandomId()),
        userId = testUser.id,
        modification = testModification,
        date = testDate)
    modificationEntityManager.addIfNew(modificationEntity)

    modificationEntity.user mustEqual testUser
    modificationEntityManager.fetchAll() mustEqual Seq(modificationEntity)
  }
}
