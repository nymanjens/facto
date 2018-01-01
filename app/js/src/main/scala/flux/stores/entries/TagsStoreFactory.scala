package flux.stores.entries

import common.GuavaReplacement.ImmutableSetMultimap
import flux.stores.entries.TagsStoreFactory.State
import models.access.DbQueryImplicits._
import models.access.{ModelField, JsEntityAccess}
import models.accounting.{BalanceCheck, Transaction}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

final class TagsStoreFactory(implicit database: JsEntityAccess) extends EntriesStoreFactory[State] {

  // **************** Public API ****************//
  def get(): Store = get((): Unit)

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    override protected def calculateState() = async {
      val transactionsWithTags: Seq[Transaction] =
        await(
          database
            .newQuery[Transaction]()
            .filter(ModelField.Transaction.tags isNotEqualTo Seq())
            .data())

      val tagToTransactionIdsBuilder =
        ImmutableSetMultimap.builder[TagsStoreFactory.Tag, TagsStoreFactory.TransactionId]()
      for {
        transaction <- transactionsWithTags
        tag <- transaction.tags
      } tagToTransactionIdsBuilder.put(tag, transaction.id)
      State(tagToTransactionIdsBuilder.build())
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      transaction.tags.nonEmpty
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
  }

  /* override */
  protected type Input = Unit
}

object TagsStoreFactory {
  type Tag = String
  type TransactionId = Long
  case class State(tagToTransactionIds: ImmutableSetMultimap[Tag, TransactionId])
      extends EntriesStore.StateTrait {
    protected override lazy val impactingTransactionIds = tagToTransactionIds.values.toSet
    protected override def impactingBalanceCheckIds = Set()
  }
}
