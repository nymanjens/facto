//package models.accounting
//
//import common.Clock
//import models._
//import org.junit.runner._
//import org.specs2.mutable._
//import org.specs2.runner._
//import play.api.test._
//
//@RunWith(classOf[JUnitRunner])
//class TransactionWithTagsTests extends Specification {
//
//  "test the Transaction and TransactionGroup models" in new WithApplication {
//
//    // prepare users
//    val user1 = Users.add(Users.newWithUnhashedPw(loginName = "tester", password = "x", name = "Tester"))
//    val user2 = Users.add(Users.newWithUnhashedPw(loginName = "tester2", password = "x", name = "Tester2"))
//
//    // get and persist dummy transaction groups
//    val transGrp1 = TransactionGroups.add(TransactionGroup())
//    val transGrp2 = TransactionGroups.add(TransactionGroup())
//    val transGrp3 = TransactionGroups.add(TransactionGroup())
//
//    // get and persist dummy transactions
//    val trans1A = Transactions.add(Transaction(
//      transactionGroupId = transGrp1.id,
//      issuerId = user1.id,
//      beneficiaryAccountCode = "ACC_A",
//      moneyReservoirCode = "CASH",
//      categoryCode = "CAT_A",
//      description = "description 1A",
//      flow = Money(300),
//      transactionDate = Clock.now,
//      consumedDate = Clock.now
//    ))
//    val trans1B = Transactions.add(Transaction(
//      transactionGroupId = transGrp1.id,
//      issuerId = user1.id,
//      beneficiaryAccountCode = "ACC_A",
//      moneyReservoirCode = "CASH",
//      categoryCode = "CAT_A",
//      description = "description 1B",
//      flow = Money(600),
//      transactionDate = Clock.now,
//      consumedDate = Clock.now
//    ))
//    val trans2 = Transactions.add(Transaction(
//      transactionGroupId = transGrp2.id,
//      issuerId = user2.id,
//      beneficiaryAccountCode = "ACC_A",
//      moneyReservoirCode = "CASH",
//      categoryCode = "CAT_A",
//      description = "description 2",
//      flow = Money(600),
//      transactionDate = Clock.now,
//      consumedDate = Clock.now
//    ))
//
//    // do checks
//    transGrp1.transactions mustEqual Seq(trans1A, trans1B)
//    transGrp2.transactions mustEqual Seq(trans2)
//    transGrp3.transactions mustEqual Seq()
//
//    trans1A.transactionGroup mustEqual transGrp1
//    trans1B.transactionGroup mustEqual transGrp1
//    trans2.transactionGroup mustEqual transGrp2
//
//    trans1A.issuer mustEqual user1
//    trans1B.issuer mustEqual user1
//    trans2.issuer mustEqual user2
//
//    Transactions.fetchAll() mustEqual Seq(trans1A, trans1B, trans2)
//  }
//}
