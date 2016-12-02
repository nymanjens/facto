package models.accounting

import com.google.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.SlickUtils.dbApi._
import common.time.Clock
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import models._
import models.accounting.money.Money

@RunWith(classOf[JUnitRunner])
class SlickTransactionManagerTest extends HookedSpecification {

  @Inject implicit val entityAccess: EntityAccess = null
  @Inject val userManager: User.Manager = null
  @Inject val balanceCheckManager: BalanceCheck.Manager = null

  @Inject val transactionManager: SlickTransactionManager = null
  @Inject val transactionGroupManager: SlickTransactionGroupManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the Transaction and TransactionGroup models" in new WithApplication {

    // prepare users
    val user1 = userManager.add(userManager.newWithUnhashedPw(loginName = "tester", password = "x", name = "Tester"))
    val user2 = userManager.add(userManager.newWithUnhashedPw(loginName = "tester2", password = "x", name = "Tester2"))

    // get and persist dummy transaction groups
    val transGrp1 = transactionGroupManager.add(TransactionGroup())
    val transGrp2 = transactionGroupManager.add(TransactionGroup())
    val transGrp3 = transactionGroupManager.add(TransactionGroup())

    // get and persist dummy transactions
    val trans1A = transactionManager.add(Transaction(
      transactionGroupId = transGrp1.id,
      issuerId = user1.id,
      beneficiaryAccountCode = "ACC_A",
      moneyReservoirCode = "CASH",
      categoryCode = "CAT_A",
      description = "description 1A",
      flowInCents = 300,
      transactionDate = Clock.now,
      consumedDate = Clock.now
    ))
    val trans1B = transactionManager.add(Transaction(
      transactionGroupId = transGrp1.id,
      issuerId = user1.id,
      beneficiaryAccountCode = "ACC_A",
      moneyReservoirCode = "CASH",
      categoryCode = "CAT_A",
      description = "description 1B",
      flowInCents = 600,
      transactionDate = Clock.now,
      consumedDate = Clock.now
    ))
    val trans2 = transactionManager.add(Transaction(
      transactionGroupId = transGrp2.id,
      issuerId = user2.id,
      beneficiaryAccountCode = "ACC_A",
      moneyReservoirCode = "CASH",
      categoryCode = "CAT_A",
      description = "description 2",
      flowInCents = 600,
      transactionDate = Clock.now,
      consumedDate = Clock.now
    ))

    // do checks
    transGrp1.transactions mustEqual Seq(trans1A, trans1B)
    transGrp2.transactions mustEqual Seq(trans2)
    transGrp3.transactions mustEqual Seq()

    trans1A.transactionGroup mustEqual transGrp1
    trans1B.transactionGroup mustEqual transGrp1
    trans2.transactionGroup mustEqual transGrp2

    trans1A.issuer mustEqual user1
    trans1B.issuer mustEqual user1
    trans2.issuer mustEqual user2

    transactionManager.fetchAll() mustEqual Seq(trans1A, trans1B, trans2)
  }
}
