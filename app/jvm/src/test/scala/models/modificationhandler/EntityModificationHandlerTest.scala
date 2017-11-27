package models.modificationhandler

import com.google.inject._
import common.GuavaReplacement.Iterables.getOnlyElement
import common.testing.TestObjects._
import common.testing._
import models._
import models.accounting.SlickTransactionManager
import models.modification.{EntityModification, SlickEntityModificationEntityManager}
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class EntityModificationHandlerTest extends HookedSpecification {

  implicit private val user = testUser

  @Inject implicit private val fakeClock: FakeClock = null
  @Inject implicit private val entityAccess: SlickEntityAccess = null
  @Inject private val transactionManager: SlickTransactionManager = null
  @Inject private val modificationEntityManager: SlickEntityModificationEntityManager = null

  @Inject private val handler: EntityModificationHandler = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "persistEntityModifications()" in {
    "Persists EntityModification" in new WithApplication {
      fakeClock.setTime(testDate)

      handler.persistEntityModifications(Seq(testModification))

      modificationEntityManager.fetchAll() must haveSize(1)
      val modificationEntity = getOnlyElement(modificationEntityManager.fetchAll())
      modificationEntity.userId mustEqual user.id
      modificationEntity.modification mustEqual testModification
      modificationEntity.date mustEqual testDate
    }

    "EntityModification.Add" in new WithApplication {
      val transaction = createTransaction()

      handler.persistEntityModifications(Seq(EntityModification.Add(transaction)))

      transactionManager.fetchAll() mustEqual Seq(transaction)
    }

    "EntityModification.Update" in new WithApplication {
      val transaction1 = createTransaction()
      val updatedTransaction1 = transaction1.copy(flowInCents = 19191)
      transactionManager.addWithId(transaction1)

      handler.persistEntityModifications(Seq(EntityModification.createUpdate(updatedTransaction1)))

      transactionManager.fetchAll() mustEqual Seq(updatedTransaction1)
    }

    "EntityModification.Delete" in new WithApplication {
      val transaction1 = createTransaction()
      transactionManager.addWithId(transaction1)

      handler.persistEntityModifications(Seq(EntityModification.createDelete(transaction1)))

      transactionManager.fetchAll() mustEqual Seq()
    }

    "EntityModification.Add is idempotent" in new WithApplication {
      val transaction1 = createTransaction()
      val updatedTransaction1 = transaction1.copy(flowInCents = 198237)
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      handler.persistEntityModifications(
        Seq(
          EntityModification.Add(transaction1),
          EntityModification.Add(transaction1),
          EntityModification.Add(updatedTransaction1),
          EntityModification.Add(transaction2)
        ))

      transactionManager.fetchAll() mustEqual Seq(transaction1, transaction2)
    }

    "EntityModification.Update is idempotent" in new WithApplication {
      val transaction1 = createTransaction()
      val updatedTransaction1 = transaction1.copy(flowInCents = 198237)
      val transaction2 = createTransaction()
      transactionManager.addWithId(transaction1)

      handler.persistEntityModifications(
        Seq(
          EntityModification.Update(updatedTransaction1),
          EntityModification.Update(updatedTransaction1),
          EntityModification.Update(transaction2)
        ))

      transactionManager.fetchAll() mustEqual Seq(updatedTransaction1)
    }

    "EntityModification.Delete is idempotent" in new WithApplication {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()
      transactionManager.addWithId(transaction1)
      transactionManager.addWithId(transaction2)

      handler.persistEntityModifications(
        Seq(
          EntityModification.createDelete(transaction2),
          EntityModification.createDelete(transaction2),
          EntityModification.createDelete(transaction3)
        ))

      transactionManager.fetchAll() mustEqual Seq(transaction1)
    }
  }
}
