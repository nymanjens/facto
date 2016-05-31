package models.accounting

import scala.util.Try

import org.joda.time.DateTime

import common.Clock
import models.SlickUtils.dbApi._
import models.SlickUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.manager.{Identifiable, EntityTable, DatabaseBackedEntityManager}
import models.{User, Users}
import models.accounting.config.Config
import models.accounting.config.{Category, Account, MoneyReservoir}

case class Transaction(transactionGroupId: Long,
                       issuerId: Long,
                       beneficiaryAccountCode: String,
                       moneyReservoirCode: String,
                       categoryCode: String,
                       description: String,
                       flow: Money,
                       detailDescription: String = "",
                       createdDate: DateTime = Clock.now,
                       transactionDate: DateTime,
                       consumedDate: DateTime,
                       idOption: Option[Long] = None) extends Identifiable[Transaction] {
  require(transactionGroupId > 0)
  require(issuerId > 0)
  require(!beneficiaryAccountCode.isEmpty)
  require(!categoryCode.isEmpty)
  require(!description.isEmpty)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  override def toString = {
    val issuerString = Try(issuer.loginName).getOrElse(issuerId.toString)
    s"Transaction(group=$transactionGroupId, issuer=${issuerString}, $beneficiaryAccountCode, $moneyReservoirCode, $categoryCode, flow=$flow, $description)"
  }

  lazy val transactionGroup: TransactionGroup = TransactionGroups.all.findById(transactionGroupId)
  lazy val issuer: User = Users.all.findById(issuerId)
  lazy val beneficiary: Account = Config.accounts(beneficiaryAccountCode)
  lazy val moneyReservoir: MoneyReservoir = Config.moneyReservoir(moneyReservoirCode)
  lazy val category: Category = Config.categories(categoryCode)

  /** Returns None if the consumed date is the same as the transaction date (and thus carries no further information. */
  def consumedDateOption: Option[DateTime] = if (consumedDate == transactionDate) None else Some(consumedDate)
}

case class TransactionPartial(beneficiary: Option[Account],
                              moneyReservoir: Option[MoneyReservoir],
                              category: Option[Category],
                              description: String,
                              flow: Money,
                              detailDescription: String = "")

object TransactionPartial {
  def from(beneficiary: Account = null,
           moneyReservoir: MoneyReservoir = null,
           category: Category = null,
           description: String = "",
           flow: Money = Money(0),
           detailDescription: String = ""): TransactionPartial =
    TransactionPartial(
      Option(beneficiary),
      Option(moneyReservoir),
      Option(category),
      description,
      flow,
      detailDescription
    )
}

class Transactions(tag: Tag) extends EntityTable[Transaction](tag, Transactions.tableName) {
  def transactionGroupId = column[Long]("transactionGroupId")
  def issuerId = column[Long]("issuerId")
  def beneficiaryAccountCode = column[String]("beneficiaryAccountCode")
  def moneyReservoirCode = column[String]("moneyReservoirCode")
  def categoryCode = column[String]("categoryCode")
  def description = column[String]("description")
  def flow = column[Money]("flow")
  def detailDescription = column[String]("detailDescription")
  def createdDate = column[DateTime]("createdDate")
  def transactionDate = column[DateTime]("transactionDate")
  def consumedDate = column[DateTime]("consumedDate")

  override def * = (transactionGroupId, issuerId, beneficiaryAccountCode, moneyReservoirCode, categoryCode, description, flow,
    detailDescription, createdDate, transactionDate, consumedDate, id.?) <>(Transaction.tupled, Transaction.unapply)
}

object Transactions {
  private val tableName: String = "TRANSACTIONS"
  val all = new DatabaseBackedEntityManager[Transaction, Transactions](tag => new Transactions(tag), tableName)
}
