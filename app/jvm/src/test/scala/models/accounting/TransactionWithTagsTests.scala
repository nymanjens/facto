package models.accounting

import com.google.inject._
import scala.collection.immutable.Seq
import common.Clock
import common.testing._
import models._
import models.accounting._
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class TransactionWithTagsTests extends HookedSpecification {

  @Inject implicit val entityAccess: EntityAccess = null
  @Inject val transactionManager: Transaction.Manager = null
  @Inject val transactionGroupManager: TransactionGroup.Manager = null
  @Inject val balanceCheckManager: BalanceCheck.Manager = null
  @Inject val tagEntityManager: TagEntity.Manager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the Transaction and TagEntity models" in new WithApplication {
    val transGrp = transactionGroupManager.add(TransactionGroup())

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
