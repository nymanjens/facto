package models.accounting

import api.ScalaJsApi.EntityType
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
                       idOption: Option[Long] = None) extends Entity {
  require(transactionGroupId > 0)
  require(issuerId > 0)
  require(!beneficiaryAccountCode.isEmpty)
  require(!categoryCode.isEmpty)
  require(!description.isEmpty)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactionGroup(implicit entityAccess: EntityAccess): TransactionGroup = entityAccess.transactionGroupManager.findById(transactionGroupId)
  def issuer(implicit entityAccess: EntityAccess): User = entityAccess.userManager.findById(issuerId)
  def beneficiary(implicit accountingConfig: Config): Account = accountingConfig.accounts(beneficiaryAccountCode)
  def moneyReservoir(implicit accountingConfig: Config): MoneyReservoir = accountingConfig.moneyReservoir(moneyReservoirCode)
  def category(implicit accountingConfig: Config): Category = accountingConfig.categories(categoryCode)
  def flow(implicit accountingConfig: Config): DatedMoney = DatedMoney(flowInCents, moneyReservoir.currency, transactionDate)
  lazy val tags: Seq[Tag] = Tag.parseTagsString(tagsString)

  /** Returns None if the consumed date is the same as the transaction date (and thus carries no further information. */
  def consumedDateOption: Option[LocalDateTime] = if (consumedDate == transactionDate) None else Some(consumedDate)

  override def toString = {
    s"Transaction(group=$transactionGroupId, issuer=${issuerId}, $beneficiaryAccountCode, $moneyReservoirCode, $categoryCode, flowInCents=$flowInCents, $description)"
  }
}
object Transaction {
  def tupled = (this.apply _).tupled

  implicit val entityType: EntityType[Transaction] = EntityType.TransactionType

  trait Manager extends EntityManager[Transaction] {
    def findByGroupId(groupId: Long): Seq[Transaction]
  }

  case class Partial(beneficiary: Option[Account],
                     moneyReservoir: Option[MoneyReservoir],
                     category: Option[Category],
                     description: String,
                     flowInCents: Long,
                     detailDescription: String = "",
                     tagsString: String = "")

  object Partial {
    def from(beneficiary: Account = null,
             moneyReservoir: MoneyReservoir = null,
             category: Category = null,
             description: String = "",
             flowInCents: Long = 0,
             detailDescription: String = "",
             tagsString: String = ""): Partial =
      Partial(
        Option(beneficiary),
        Option(moneyReservoir),
        Option(category),
        description,
        flowInCents,
        detailDescription,
        tagsString
      )
  }
}
