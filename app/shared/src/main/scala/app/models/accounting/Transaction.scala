package app.models.accounting

import app.common.money.DatedMoney
import app.models.access.AppEntityAccess
import app.models.accounting.config.Account
import app.models.accounting.config.Category
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.Transaction.Attachment
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.common.time.LocalDateTime
import hydro.common.GuavaReplacement
import hydro.models.Entity

import scala.collection.immutable.Seq

/** Transaction entities are immutable. Just delete and create a new one when updating. */
case class Transaction(
    transactionGroupId: Long,
    issuerId: Long,
    beneficiaryAccountCode: String,
    moneyReservoirCode: String,
    categoryCode: String,
    description: String,
    flowInCents: Long,
    detailDescription: String,
    tags: Seq[String],
    attachments: Seq[Attachment],
    createdDate: LocalDateTime,
    transactionDate: LocalDateTime,
    consumedDate: LocalDateTime,
    idOption: Option[Long] = None,
) extends Entity {
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
  implicit val Type: EntityType[Transaction] = EntityType()

  def tupled = (this.apply _).tupled

  case class Attachment(contentHash: String, filename: String, fileType: String, fileSizeBytes: Int) {
    def toEncodedString(): String = {
      val typeEncoded = fileType.replace('/', '>')
      f"$contentHash/$typeEncoded/$fileSizeBytes/$filename"
    }
  }
  object Attachment {
    def fromEncodedString(string: String): Attachment = {
      GuavaReplacement.Splitter.on('/').split(string) match {
        case Seq(contentHash, typeEncoded, fileSizeBytes, filename) =>
          Attachment(
            contentHash = contentHash,
            filename = filename,
            fileType = typeEncoded.replace('>', '/'),
            fileSizeBytes = fileSizeBytes.toInt,
          )
      }

    }
  }

  /** Same as Transaction, except all fields are optional. */
  case class Partial(
      transactionGroupId: Option[Long] = None,
      issuerId: Option[Long] = None,
      beneficiary: Option[Account] = None,
      moneyReservoir: Option[MoneyReservoir] = None,
      category: Option[Category] = None,
      description: String = "",
      flowInCents: Long = 0,
      detailDescription: String = "",
      tags: Seq[String] = Seq(),
      attachments: Seq[Attachment] = Seq(),
      createdDate: Option[LocalDateTime] = None,
      transactionDate: Option[LocalDateTime] = None,
      consumedDate: Option[LocalDateTime] = None,
      idOption: Option[Long] = None,
  ) {
    def issuer(implicit entityAccess: AppEntityAccess): Option[User] =
      issuerId.map(entityAccess.newQuerySyncForUser().findById)
    def isEmpty: Boolean = this == Partial.empty
  }

  object Partial {
    val empty: Partial = Partial()

    def from(
        transactionGroupId: Long = 0,
        issuerId: Long = 0,
        beneficiary: Account = null,
        moneyReservoir: MoneyReservoir = null,
        category: Category = null,
        description: String = "",
        flowInCents: Long = 0,
        detailDescription: String = "",
        tags: Seq[String] = Seq(),
        attachments: Seq[Attachment] = Seq(),
        createdDate: LocalDateTime = null,
        transactionDate: LocalDateTime = null,
        consumedDate: LocalDateTime = null,
        idOption: Option[Long] = None,
    ): Partial =
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
        attachments = attachments,
        createdDate = Option(createdDate),
        transactionDate = Option(transactionDate),
        consumedDate = Option(consumedDate),
        idOption = idOption,
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
        attachments = transaction.attachments,
        createdDate = transaction.createdDate,
        transactionDate = transaction.transactionDate,
        consumedDate = transaction.consumedDate,
        idOption = transaction.idOption,
      )
  }
}
