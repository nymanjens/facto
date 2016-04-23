package models.accounting

import scala.util.Try

import org.joda.time.DateTime
import slick.driver.H2Driver.api._

import common.Clock
import models.{User, Users}
import models.ModelUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.accounting.config.Config
import models.accounting.config.{Category, Account, MoneyReservoir}
import models.activeslick._

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
                       id: Option[Long] = None) extends Identifiable[Transaction] {
  require(transactionGroupId > 0)
  require(issuerId > 0)
  require(!beneficiaryAccountCode.isEmpty)
  require(!categoryCode.isEmpty)
  require(!description.isEmpty)
  for (idVal <- id) require(idVal > 0)

  override def withId(id: Long) = copy(id = Some(id))

  override def toString = {
    val issuerString = Try(issuer.loginName).getOrElse(issuerId.toString)
    s"Transaction(group=$transactionGroupId, issuer=${issuerString}, $beneficiaryAccountCode, $moneyReservoirCode, $categoryCode, flow=$flow, $description)"
  }

  lazy val transactionGroup: TransactionGroup = TransactionGroups.all.findById(transactionGroupId)
  lazy val issuer: User = Users.all.findById(issuerId)
  lazy val beneficiary: Account = Config.accounts(beneficiaryAccountCode)
  lazy val moneyReservoir: MoneyReservoir = Config.moneyReservoirs(moneyReservoirCode)
  lazy val category: Category = Config.categories(categoryCode)

  /** Returns None if the consumed date is the same as the transaction date (and thus carries no further information. */
  def consumedDateOption: Option[DateTime] = if (consumedDate == transactionDate) None else Some(consumedDate)
}

class Transactions(tag: Tag) extends EntityTable[Transaction](tag, "TRANSACTIONS") {
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

  def * = (transactionGroupId, issuerId, beneficiaryAccountCode, moneyReservoirCode, categoryCode, description, flow, detailDescription, createdDate, transactionDate, consumedDate, id.?) <>(Transaction.tupled, Transaction.unapply)
}

object Transactions {
  val all = new EntityTableQuery[Transaction, Transactions](tag => new Transactions(tag))
}
