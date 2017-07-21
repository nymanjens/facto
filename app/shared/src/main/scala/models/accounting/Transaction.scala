package models.accounting

import models.manager.EntityType
import common.time.Clock
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money.{DatedMoney, Money}
import models.manager.{Entity, EntityManager}
import models._
import common.time.LocalDateTime

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.util.Try

/** Transaction entities are immutable. Just delete and create a new one when updating. */
case class Transaction(transactionGroupId: Long,
                       issuerId: Long,
                       beneficiaryAccountCode: String,
                       moneyReservoirCode: String,
                       categoryCode: String,
                       description: String,
                       flowInCents: Long,
                       detailDescription: String = "",
                       tagsString: String = "",
                       createdDate: LocalDateTime,
                       transactionDate: LocalDateTime,
                       consumedDate: LocalDateTime,
                       idOption: Option[Long] = None)
    extends Entity {
  require(transactionGroupId > 0)
  require(issuerId > 0)
  require(!beneficiaryAccountCode.isEmpty)
  require(!categoryCode.isEmpty)
  require(!description.isEmpty)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactionGroup(implicit entityAccess: EntityAccess): TransactionGroup =
    entityAccess.transactionGroupManager.findById(transactionGroupId)
  def issuer(implicit entityAccess: EntityAccess): User = entityAccess.userManager.findById(issuerId)
  def beneficiary(implicit accountingConfig: Config): Account =
    accountingConfig.accounts(beneficiaryAccountCode)
  def moneyReservoir(implicit accountingConfig: Config): MoneyReservoir =
    accountingConfig.moneyReservoir(moneyReservoirCode)
  def category(implicit accountingConfig: Config): Category = accountingConfig.categories(categoryCode)
  def flow(implicit accountingConfig: Config): DatedMoney =
    DatedMoney(flowInCents, moneyReservoir.currency, transactionDate)
  lazy val tags: Seq[Tag] = Tag.parseTagsString(tagsString)

  /** Returns None if the consumed date is the same as the transaction date (and thus carries no further information. */
  def consumedDateOption: Option[LocalDateTime] =
    if (consumedDate == transactionDate) None else Some(consumedDate)

  override def toString = {
    s"Transaction(group=$transactionGroupId, issuer=${issuerId}, $beneficiaryAccountCode, $moneyReservoirCode, $categoryCode, flowInCents=$flowInCents, $description)"
  }
}
object Transaction {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[Transaction] {
    def findByGroupId(groupId: Long): Seq[Transaction]
  }

  /** Same as Transaction, except all fields are optional. */
  case class Partial(transactionGroupId: Option[Long] = None,
                     issuerId: Option[Long] = None,
                     beneficiary: Option[Account] = None,
                     moneyReservoir: Option[MoneyReservoir] = None,
                     category: Option[Category] = None,
                     description: String = "",
                     flowInCents: Long = 0,
                     detailDescription: String = "",
                     tagsString: String = "",
                     createdDate: Option[LocalDateTime] = None,
                     transactionDate: Option[LocalDateTime] = None,
                     consumedDate: Option[LocalDateTime] = None,
                     idOption: Option[Long] = None)

  object Partial {
    def from(transactionGroupId: Long = 0,
             issuerId: Long = 0,
             beneficiary: Account = null,
             moneyReservoir: MoneyReservoir = null,
             category: Category = null,
             description: String = "",
             flowInCents: Long = 0,
             detailDescription: String = "",
             tagsString: String = "",
             createdDate: LocalDateTime = null,
             transactionDate: LocalDateTime = null,
             consumedDate: LocalDateTime = null,
             idOption: Option[Long] = None): Partial =
      Partial(
        transactionGroupId = if (transactionGroupId == 0) None else Some(transactionGroupId),
        issuerId = if (issuerId == 0) None else Some(issuerId),
        beneficiary = Option(beneficiary),
        moneyReservoir = Option(moneyReservoir),
        category = Option(category),
        description = description,
        flowInCents = flowInCents,
        detailDescription = detailDescription,
        tagsString = tagsString,
        createdDate = Option(createdDate),
        transactionDate = Option(transactionDate),
        consumedDate = Option(consumedDate),
        idOption = idOption
      )

    def from(transaction: Transaction)(implicit accountingConfig: Config): Partial =
      from(
        transactionGroupId = transaction.transactionGroupId,
        issuerId = transaction.issuerId,
        beneficiary = transaction.beneficiary,
        moneyReservoir = transaction.moneyReservoir,
        category = transaction.category,
        description = transaction.description,
        flowInCents = transaction.flowInCents,
        detailDescription = transaction.detailDescription,
        tagsString = transaction.tagsString,
        createdDate = transaction.createdDate,
        transactionDate = transaction.transactionDate,
        consumedDate = transaction.consumedDate,
        idOption = transaction.idOption
      )
  }
}
