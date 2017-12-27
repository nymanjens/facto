package models.accounting

import com.google.inject._
import common.testing._
import common.time.Clock
import models._
import models.user.SlickUserManager
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class SlickTransactionManagerTest extends HookedSpecification {

  @Inject implicit private val clock: Clock = null
  @Inject implicit private val entityAccess: EntityAccess = null
  @Inject private val userManager: SlickUserManager = null

  @Inject private val transactionManager: SlickTransactionManager = null
  @Inject private val transactionGroupManager: SlickTransactionGroupManager = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
  }

  "test the Transaction and TransactionGroup models" in new WithApplication {

    // prepare users
    val user1 =
      TestUtils.persist(SlickUserManager.createUser(loginName = "tester", password = "x", name = "Tester"))
    val user2 =
      TestUtils.persist(SlickUserManager.createUser(loginName = "tester2", password = "x", name = "Tester2"))

    // get and persist dummy transaction groups
    val transGrp1 = TestUtils.persist(TransactionGroup(createdDate = clock.now))
    val transGrp2 = TestUtils.persist(TransactionGroup(createdDate = clock.now))
    val transGrp3 = TestUtils.persist(TransactionGroup(createdDate = clock.now))

    // get and persist dummy transactions
    val trans1A = TestUtils.persist(
      Transaction(
        transactionGroupId = transGrp1.id,
        issuerId = user1.id,
        beneficiaryAccountCode = "ACC_A",
        moneyReservoirCode = "CASH",
        categoryCode = "CAT_A",
        description = "description 1A",
        flowInCents = 300,
        createdDate = clock.now,
        transactionDate = clock.now,
        consumedDate = clock.now
      ))
    val trans1B = TestUtils.persist(
      Transaction(
        transactionGroupId = transGrp1.id,
        issuerId = user1.id,
        beneficiaryAccountCode = "ACC_A",
        moneyReservoirCode = "CASH",
        categoryCode = "CAT_A",
        description = "description 1B",
        flowInCents = 600,
        createdDate = clock.now,
        transactionDate = clock.now,
        consumedDate = clock.now
      ))
    val trans2 = TestUtils.persist(
      Transaction(
        transactionGroupId = transGrp2.id,
        issuerId = user2.id,
        beneficiaryAccountCode = "ACC_A",
        moneyReservoirCode = "CASH",
        categoryCode = "CAT_A",
        description = "description 2",
        flowInCents = 600,
        createdDate = clock.now,
        transactionDate = clock.now,
        consumedDate = clock.now
      ))

    // do checks
    transGrp1.transactions.toSet mustEqual Set(trans1A, trans1B)
    transGrp2.transactions mustEqual Seq(trans2)
    transGrp3.transactions mustEqual Seq()

    trans1A.transactionGroup mustEqual transGrp1
    trans1B.transactionGroup mustEqual transGrp1
    trans2.transactionGroup mustEqual transGrp2

    trans1A.issuer mustEqual user1
    trans1B.issuer mustEqual user1
    trans2.issuer mustEqual user2

    transactionManager.fetchAll().toSet mustEqual Set(trans1A, trans1B, trans2)
  }
}
