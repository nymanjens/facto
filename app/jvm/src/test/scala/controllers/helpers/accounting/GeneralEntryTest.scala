package controllers.helpers.accounting

import com.google.inject._
import common.testing._
import scala.collection.immutable.Seq
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import org.joda.time.DateTime
import models.accounting._
import models.accounting.config.{Account, Category, MoneyReservoir}
import common.testing.TestObjects._
import common.testing.TestObjects.accountingConfig.constants.endowmentCategory
import common.testing.TestUtils._
import models.accounting.money.Money
import models._

@RunWith(classOf[JUnitRunner])
class GeneralEntryTest extends HookedSpecification {

  @Inject val generalEntries: GeneralEntries = null
  @Inject implicit val entityAccess: EntityAccess = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "fetchLastNEntries()" in new WithApplication {
    // Get and persist dummy transactions
    val trans1 = persistTransaction(groupId = 1, flowInCents = 200, timestamp = 1000)
    val trans2 = persistTransaction(groupId = 2, flowInCents = 300, timestamp = 1020)
    val trans3 = persistTransaction(groupId = 2, flowInCents = 100, timestamp = 1030)
    val trans4 = persistTransaction(groupId = 1, flowInCents = -200, timestamp = 1080)
    val trans5 = persistTransaction(groupId = 5, flowInCents = -50, timestamp = 1090)
    val trans6 = persistTransaction(groupId = 6, flowInCents = -30, timestamp = 1100)

    // Get expectations
    val expectedEntries = Vector(
      GeneralEntry(Seq(trans1)),
      GeneralEntry(Seq(trans2, trans3)),
      GeneralEntry(Seq(trans4)),
      GeneralEntry(Seq(trans5)),
      GeneralEntry(Seq(trans6))
    )

    // Run tests
    for (i <- 1 to expectedEntries.size) {
      val subList = expectedEntries.takeRight(i)
      generalEntries.fetchLastNEntries(n = subList.size) mustEqual subList
    }

    // Test when n > num entries
    generalEntries.fetchLastNEntries(n = 1000) mustEqual expectedEntries
  }

  "fetchLastNEndowments()" in new WithApplication {
    // Get and persist dummy transactions
    // Irrelevant transactions (should be ignored)
    persistTransaction(groupId = 91, account = testAccountB)
    persistTransaction(groupId = 92, account = testAccountA, category = testCategory)

    // Relevant
    val trans1 = persistTransaction(groupId = 1, flowInCents = 201, timestamp = 1000, account = testAccountA, category = endowmentCategory)
    val trans2 = persistTransaction(groupId = 2, flowInCents = 202, timestamp = 1010, account = testAccountA, category = endowmentCategory)
    val trans3 = persistTransaction(groupId = 2, flowInCents = 203, timestamp = 1020, account = testAccountA, category = endowmentCategory)
    val trans4 = persistTransaction(groupId = 3, flowInCents = 204, timestamp = 1030, account = testAccountA, category = endowmentCategory)
    val trans5 = persistTransaction(groupId = 4, flowInCents = 205, timestamp = 1040, account = testAccountA, category = endowmentCategory)
    val trans6 = persistTransaction(groupId = 5, flowInCents = 206, timestamp = 1050, account = testAccountA, category = endowmentCategory)

    // Get expectations
    val expectedEntries = Vector(
      GeneralEntry(Seq(trans1)),
      GeneralEntry(Seq(trans2, trans3)),
      GeneralEntry(Seq(trans4)),
      GeneralEntry(Seq(trans5)),
      GeneralEntry(Seq(trans6))
    )

    // Run tests
    for (i <- 1 to expectedEntries.size) {
      val subList = expectedEntries.takeRight(i)
      generalEntries.fetchLastNEndowments(testAccountA, n = subList.size) mustEqual subList
    }

    // test when n > num entries
    generalEntries.fetchLastNEndowments(testAccountA, n = 1000) mustEqual expectedEntries
  }

  "search()" should {
    "Matches tags, description and detailDescription" in new WithApplication {
      // Get and persist dummy transactions
      val trans1 = persistTransaction(groupId = 1)
      val trans2 = persistTransaction(groupId = 2, description = "abc")
      val trans3 = persistTransaction(groupId = 2, detailDescription = "abcd")
      val trans4 = persistTransaction(groupId = 3, tagsString = "abc def")

      // Get expectations
      val expectedEntries = Vector(
        GeneralEntry(Seq(trans4)),
        GeneralEntry(Seq(trans2, trans3))
      )

      generalEntries.search("def abc") mustEqual expectedEntries
    }

    "Match the Money flow" in new WithApplication {
      // Get and persist dummy transactions
      val trans1 = persistTransaction(flowInCents = 999)
      val trans2 = persistTransaction(flowInCents = -1234)
      val trans3 = persistTransaction(flowInCents = 91234)
      val trans4 = persistTransaction(flowInCents = 1234, timestamp = 1010)
      val trans5 = persistTransaction(flowInCents = 1234, timestamp = 1020)

      // Get expectations
      val expectedEntries = Vector(
        GeneralEntry(Seq(trans5)),
        GeneralEntry(Seq(trans4)),
        GeneralEntry(Seq(trans2)),
        GeneralEntry(Seq(trans3))
      )

      generalEntries.search("12.34") mustEqual expectedEntries
    }
  }
}
