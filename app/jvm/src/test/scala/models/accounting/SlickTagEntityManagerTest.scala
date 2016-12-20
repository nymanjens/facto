package models.accounting

import com.google.inject._
import common.testing._
import models._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class SlickTagEntityManagerTest extends HookedSpecification {

  @Inject implicit private val fakeClock: FakeClock = null
  @Inject implicit private val entityAccess: EntityAccess = null
  @Inject private val transactionManager: Transaction.Manager = null
  @Inject private val transactionGroupManager: TransactionGroup.Manager = null

  @Inject private val tagEntityManager: SlickTagEntityManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the Transaction and TagEntity models" in new WithApplication {
    val transGrp = transactionGroupManager.add(TransactionGroup(createdDate = fakeClock.now))

    // Add dummy transaction
    val trans = TestUtils.persistTransaction(tagsString = "a,b")

    // Do checks
    val tagEntities = tagEntityManager.fetchAll()
    getWithTag("a", tagEntities).transactionId mustEqual trans.id
    getWithTag("b", tagEntities).transactionId mustEqual trans.id

    // Remove transaction
    transactionManager.delete(trans)

    // Do checks
    tagEntityManager.fetchAll() must beEmpty
  }

  private def getWithTag(name: String, tagEntities: Seq[TagEntity]): TagEntity = {
    val filteredEntities = tagEntities.filter(_.name == name)
    filteredEntities must haveSize(1)
    filteredEntities.head
  }
}
