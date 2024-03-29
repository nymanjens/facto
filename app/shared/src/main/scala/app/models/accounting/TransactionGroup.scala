package app.models.accounting

import app.common.money.CurrencyValueManager
import app.common.money.ReferenceMoney
import app.models.access.AppDbQuerySorting
import app.models.access.ModelFields
import app.models.accounting.config.Config
import hydro.models.modification.EntityType
import hydro.common.time.LocalDateTime
import hydro.models.Entity
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.EntityAccess

import scala.collection.immutable.Seq
import scala.concurrent.Future

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: LocalDateTime, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions(implicit entityAccess: EntityAccess): Future[Seq[Transaction]] = {
    entityAccess
      .newQuery[Transaction]()
      .filter(ModelFields.Transaction.transactionGroupId === id)
      .sort(AppDbQuerySorting.Transaction.deterministicallyByCreateDate)
      .data()
  }
}

object TransactionGroup {
  implicit val Type: EntityType[TransactionGroup] = EntityType()

  def tupled = (this.apply _).tupled

  /**
   * Same as TransactionGroup, except all fields are optional, plus an additional `transactions` and `zeroSum` which
   * a `TransactionGroup` stores implicitly.
   */
  case class Partial(
      transactions: Seq[Transaction.Partial],
      zeroSum: Boolean = false,
      createdDate: Option[LocalDateTime] = None,
      idOption: Option[Long] = None,
  )
  object Partial {
    val withSingleEmptyTransaction: Partial = Partial(transactions = Seq(Transaction.Partial.empty))

    def from(group: TransactionGroup, transactions: Seq[Transaction])(implicit
        accountingConfig: Config,
        currencyValueManager: CurrencyValueManager,
    ): Partial = {
      val isZeroSum = transactions.map(_.flow.exchangedForReferenceCurrency()).sum == ReferenceMoney(0)
      Partial(
        transactions = transactions.map(Transaction.Partial.from),
        zeroSum = isZeroSum,
        createdDate = Some(group.createdDate),
        idOption = group.idOption,
      )
    }
  }
}
