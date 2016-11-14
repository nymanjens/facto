package models.accounting

import com.google.common.base.Splitter
import com.google.common.hash.{HashCode, Hashing}
import common.Clock
import models.SlickUtils.JodaToSqlDateMapper
import models.SlickUtils.dbRun
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money.{DatedMoney, Money}
import models.manager.{Entity, SlickEntityManager, EntityTable, ImmutableEntityManager}
import models._
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.util.Try

/** Transactions entities are immutable. Just delete and create a new one when updating. */
case class Transaction(transactionGroupId: Long,
                       issuerId: Long,
                       beneficiaryAccountCode: String,
                       moneyReservoirCode: String,
                       categoryCode: String,
                       description: String,
                       private val flowInCents: Long,
                       detailDescription: String = "",
                       tagsString: String = "",
                       createdDate: DateTime = Clock.now,
                       transactionDate: DateTime,
                       consumedDate: DateTime,
                       idOption: Option[Long] = None) extends Entity[Transaction] {
  require(transactionGroupId > 0)
  require(issuerId > 0)
  require(!beneficiaryAccountCode.isEmpty)
  require(!categoryCode.isEmpty)
  require(!description.isEmpty)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  lazy val transactionGroup: TransactionGroup = TransactionGroups.findById(transactionGroupId)
  def issuer(implicit entityAccess: EntityAccess): User = entityAccess.userManager.findById(issuerId)
  def beneficiary(implicit accountingConfig: Config): Account = accountingConfig.accounts(beneficiaryAccountCode)
  def moneyReservoir(implicit accountingConfig: Config): MoneyReservoir = accountingConfig.moneyReservoir(moneyReservoirCode)
  def category(implicit accountingConfig: Config): Category = accountingConfig.categories(categoryCode)
  def flow(implicit accountingConfig: Config): DatedMoney = DatedMoney(flowInCents, moneyReservoir.currency, transactionDate)
  lazy val tags: Seq[Tag] = Tag.parseTagsString(tagsString)

  /** Returns None if the consumed date is the same as the transaction date (and thus carries no further information. */
  def consumedDateOption: Option[DateTime] = if (consumedDate == transactionDate) None else Some(consumedDate)

  override def toString = {
    s"Transaction(group=$transactionGroupId, issuer=${issuerId}, $beneficiaryAccountCode, $moneyReservoirCode, $categoryCode, flowInCents=$flowInCents, $description)"
  }
}

case class TransactionPartial(beneficiary: Option[Account],
                              moneyReservoir: Option[MoneyReservoir],
                              category: Option[Category],
                              description: String,
                              flowInCents: Long,
                              detailDescription: String = "",
                              tagsString: String = "")

object TransactionPartial {
  def from(beneficiary: Account = null,
           moneyReservoir: MoneyReservoir = null,
           category: Category = null,
           description: String = "",
           flowInCents: Long = 0,
           detailDescription: String = "",
           tagsString: String = ""): TransactionPartial =
    TransactionPartial(
      Option(beneficiary),
      Option(moneyReservoir),
      Option(category),
      description,
      flowInCents,
      detailDescription,
      tagsString
    )
}

class Transactions(tag: SlickTag) extends EntityTable[Transaction](tag, Transactions.tableName) {
  def transactionGroupId = column[Long]("transactionGroupId")
  def issuerId = column[Long]("issuerId")
  def beneficiaryAccountCode = column[String]("beneficiaryAccountCode")
  def moneyReservoirCode = column[String]("moneyReservoirCode")
  def categoryCode = column[String]("categoryCode")
  def description = column[String]("description")
  def flow = column[Long]("flow")
  def detailDescription = column[String]("detailDescription")
  def tagsString = column[String]("tagsString")
  def createdDate = column[DateTime]("createdDate")
  def transactionDate = column[DateTime]("transactionDate")
  def consumedDate = column[DateTime]("consumedDate")

  override def * = (transactionGroupId, issuerId, beneficiaryAccountCode, moneyReservoirCode, categoryCode, description, flow,
    detailDescription, tagsString, createdDate, transactionDate, consumedDate, id.?) <> (Transaction.tupled, Transaction.unapply)
}

object Transactions extends ImmutableEntityManager[Transaction, Transactions](
  SlickEntityManager.create[Transaction, Transactions](
    tag => new Transactions(tag), tableName = "TRANSACTIONS")) {

  // ********** Mutators ********** //
  // Overriding mutators to update the TagEntities table
  override def add(transaction: Transaction): Transaction = {
    val persistedTransaction = super.add(transaction)

    // Add TagEntities to database
    for (tag <- persistedTransaction.tags) {
      TagEntities.add(
        TagEntity(
          name = tag.name,
          transactionId = persistedTransaction.id))
    }

    persistedTransaction
  }

  override def delete(transaction: Transaction): Unit = {
    // Remove TagEntities from database
    val tags = dbRun(TagEntities.newQuery.filter(_.transactionId === transaction.id))
    for (tag <- tags) {
      TagEntities.delete(tag)
    }

    super.delete(transaction)
  }
}
