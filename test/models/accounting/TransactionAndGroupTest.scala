package models.accounting

import scala.concurrent.ExecutionContext.Implicits.global

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import org.joda.time.DateTime
import slick.driver.H2Driver.api._

import common.Clock
import common.testing.TestObjects._
import common.testing.TestUtils._
import models._

@RunWith(classOf[JUnitRunner])
class TransactionAndGroupTests extends Specification {

  "test the Transaction and TransactionGroup models" in new WithApplication(fakeApplication) {

    // prepare users
    val user1 = Users.all.save(Users.newWithUnhashedPw(loginName = "tester", password = "x", name = "Tester"))
    val user2 = Users.all.save(Users.newWithUnhashedPw(loginName = "tester2", password = "x", name = "Tester2"))

    // get and persist dummy transaction groups
    val transGrp1 = TransactionGroups.all.save(TransactionGroup())
    val transGrp2 = TransactionGroups.all.save(TransactionGroup())
    val transGrp3 = TransactionGroups.all.save(TransactionGroup())

    // get and persist dummy transactions
    val trans1A = Transactions.all.save(Transaction(
      transactionGroupId = transGrp1.id.get,
      issuerId = user1.id.get,
      beneficiaryAccountCode = "ACC_A",
      moneyReservoirCode = "CASH",
      categoryCode = "CAT_A",
      description = "description 1A",
      flow = Money(300),
      transactionDate = Clock.now,
      consumedDate = Clock.now
    ))
    val trans1B = Transactions.all.save(Transaction(
      transactionGroupId = transGrp1.id.get,
      issuerId = user1.id.get,
      beneficiaryAccountCode = "ACC_A",
      moneyReservoirCode = "CASH",
      categoryCode = "CAT_A",
      description = "description 1B",
      flow = Money(600),
      transactionDate = Clock.now,
      consumedDate = Clock.now
    ))
    val trans2 = Transactions.all.save(Transaction(
      transactionGroupId = transGrp2.id.get,
      issuerId = user2.id.get,
      beneficiaryAccountCode = "ACC_A",
      moneyReservoirCode = "CASH",
      categoryCode = "CAT_A",
      description = "description 2",
      flow = Money(600),
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

    Transactions.all.fetchAll mustEqual Seq(trans1A, trans1B, trans2)
  }
}
