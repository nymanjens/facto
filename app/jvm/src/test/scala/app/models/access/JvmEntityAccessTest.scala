package app.models.access

import com.google.inject._
import common.GuavaReplacement.Iterables.getOnlyElement
import common.testing.TestObjects._
import common.testing._
import app.models.accounting.Transaction
import app.models.modification.EntityModification
import app.models.modification.EntityModificationEntity
import app.models.slick.SlickUtils.dbRun
import app.models.user.User
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class JvmEntityAccessTest extends HookedSpecification {

  implicit private val user = testUser

  @Inject implicit private val fakeClock: FakeClock = null

  @Inject private val entityAccess: JvmEntityAccess = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "persistEntityModifications()" in {
    "Persists EntityModification" in new WithApplication {
      fakeClock.setNowInstant(testInstant)

      entityAccess.persistEntityModifications(testModification)

      val modificationEntity = getOnlyElement(allEntityModifications())
      modificationEntity.userId mustEqual user.id
      modificationEntity.modification mustEqual testModification
      modificationEntity.instant mustEqual testInstant
    }

    "EntityModification.Add" in new WithApplication {
      val transaction = createTransaction()

      entityAccess.persistEntityModifications(EntityModification.Add(transaction))

      entityAccess.newQuerySync[Transaction]().data() mustEqual Seq(transaction)
    }

    "EntityModification.Update" in new WithApplication {
      val user1 = createUser()
      val updatedUser1 = user1.copy(name = "other nme")
      entityAccess.persistEntityModifications(EntityModification.Add(user1))

      entityAccess.persistEntityModifications(EntityModification.createUpdate(updatedUser1))

      entityAccess.newQuerySync[User]().data() mustEqual Seq(updatedUser1)
    }

    "EntityModification.Delete" in new WithApplication {
      val transaction1 = createTransaction()
      entityAccess.persistEntityModifications(EntityModification.Add(transaction1))

      entityAccess.persistEntityModifications(EntityModification.createDelete(transaction1))

      entityAccess.newQuerySync[Transaction]().data() mustEqual Seq()
    }

    "EntityModification.Add is idempotent" in new WithApplication {
      val transaction1 = createTransaction()
      val updatedTransaction1 = transaction1.copy(flowInCents = 198237)
      val transaction2 = createTransaction()

      entityAccess.persistEntityModifications(
        EntityModification.Add(transaction1),
        EntityModification.Add(transaction1),
        EntityModification.Add(updatedTransaction1),
        EntityModification.Add(transaction2)
      )

      entityAccess.newQuerySync[Transaction]().data().toSet mustEqual Set(transaction1, transaction2)
    }

    "EntityModification.Update is idempotent" in new WithApplication {
      val user1 = createUser()
      val updatedUser1 = user1.copy(name = "other nme")
      val user2 = createUser()
      entityAccess.persistEntityModifications(EntityModification.Add(user1))

      entityAccess.persistEntityModifications(
        EntityModification.Update(updatedUser1),
        EntityModification.Update(updatedUser1),
        EntityModification.Update(user2)
      )

      entityAccess.newQuerySync[User]().data() mustEqual Seq(updatedUser1)
    }

    "EntityModification.Delete is idempotent" in new WithApplication {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()
      entityAccess.persistEntityModifications(EntityModification.Add(transaction1))
      entityAccess.persistEntityModifications(EntityModification.Add(transaction2))

      entityAccess.persistEntityModifications(
        EntityModification.createDelete(transaction2),
        EntityModification.createDelete(transaction2),
        EntityModification.createDelete(transaction3)
      )

      entityAccess.newQuerySync[Transaction]().data() mustEqual Seq(transaction1)
    }

    "Filters duplicates: EntityModification.Add" in new WithApplication {
      val user1 = createUser()
      val updatedUser1 = user1.copy(name = "other name")
      entityAccess.persistEntityModifications(EntityModification.Add(user1))
      entityAccess.persistEntityModifications(EntityModification.Update(updatedUser1))
      val initialModifications = allEntityModifications()

      entityAccess.persistEntityModifications(EntityModification.Add(user1), EntityModification.Add(user1))

      entityAccess.newQuerySync[User]().data() mustEqual Seq(updatedUser1)
      allEntityModifications() mustEqual initialModifications
    }
    "Filters duplicates: EntityModification.Update" in new WithApplication {
      val user1 = createUser()
      val updatedUser1 = user1.copy(name = "other name")

      entityAccess.persistEntityModifications(EntityModification.Add(user1))
      entityAccess.persistEntityModifications(EntityModification.createDelete(user1))
      val initialModifications = allEntityModifications()

      entityAccess.persistEntityModifications(EntityModification.Update(updatedUser1))

      entityAccess.newQuerySync[User]().data() must beEmpty
      allEntityModifications() mustEqual initialModifications
    }
  }

  private def allEntityModifications(): Seq[EntityModificationEntity] =
    dbRun(entityAccess.newSlickQuery[EntityModificationEntity]()).toVector

  private def createUser(): User = testUser.copy(idOption = Some(EntityModification.generateRandomId()))
}
