package app.models.accounting

import app.common.money.DatedMoney
import hydro.common.time.LocalDateTime
import app.models._
import app.models.access.AppEntityAccess
import app.models.accounting.config.Account
import app.models.accounting.config.Category
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.user.User

import scala.collection.immutable.Seq

/** Transaction entities are immutable. Just delete and create a new one when updating. */
case class Transaction(transactionGroupId: Long,
                       issuerId: Long,
                       beneficiaryAccountCode: String,
                       moneyReservoirCode: String,
                       categoryCode: String,
                       description: String,
                       flowInCents: Long,
                       detailDescription: String = "",
                       tags: Seq[String] = Seq(),
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

  def issuer(implicit entityAccess: AppEntityAccess): User =
    entityAccess.newQuerySyncForUser().findById(issuerId)
  def beneficiary(implicit accountingConfig: Config): Account =
    accountingConfig.accounts(beneficiaryAccountCode)
  def moneyReservoir(implicit accountingConfig: Config): MoneyReservoir =
    accountingConfig.moneyReservoir(moneyReservoirCode)
  def category(implicit accountingConfig: Config): Category = accountingConfig.categories(categoryCode)
  def flow(implicit accountingConfig: Config): DatedMoney =
    DatedMoney(flowInCents, moneyReservoir.currency, transactionDate)

  /** Returns None if the consumed date is the same as the transaction date (and thus carries no further information. */
  def consumedDateOption: Option[LocalDateTime] =
    if (consumedDate == transactionDate) None else Some(consumedDate)

  override def toString = {
    s"Transaction(id=$idOption, group=$transactionGroupId, issuer=${issuerId}, $beneficiaryAccountCode, " +
      s"$moneyReservoirCode, $categoryCode, flowInCents=$flowInCents, $description, createdDate=$createdDate)"
  }
}
object Transaction {
  def tupled = (this.apply _).tupled

  /** Same as Transaction, except all fields are optional. */
  case class Partial(transactionGroupId: Option[Long] = None,
                     issuerId: Option[Long] = None,
                     beneficiary: Option[Account] = None,
                     moneyReservoir: Option[MoneyReservoir] = None,
                     category: Option[Category] = None,
                     description: String = "",
                     flowInCents: Long = 0,
                     detailDescription: String = "",
                     tags: Seq[String] = Seq(),
                     createdDate: Option[LocalDateTime] = None,
                     transactionDate: Option[LocalDateTime] = None,
                     consumedDate: Option[LocalDateTime] = None,
                     idOption: Option[Long] = None) {
    def issuer(implicit entityAccess: AppEntityAccess): Option[User] =
      issuerId.map(entityAccess.newQuerySyncForUser().findById)
    def isEmpty: Boolean = this == Partial.empty
  }

  object Partial {
    val empty: Partial = Partial()

    def from(transactionGroupId: Long = 0,
             issuerId: Long = 0,
             beneficiary: Account = null,
             moneyReservoir: MoneyReservoir = null,
             category: Category = null,
             description: String = "",
             flowInCents: Long = 0,
             detailDescription: String = "",
             tags: Seq[String] = Seq(),
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
        tags = tags,
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
        tags = transaction.tags,
        createdDate = transaction.createdDate,
        transactionDate = transaction.transactionDate,
        consumedDate = transaction.consumedDate,
        idOption = transaction.idOption
      )
  }
}
