package app.flux.stores.entries.factories

import hydro.common.GuavaReplacement.ImmutableSetMultimap
import app.flux.stores.entries.EntriesStore
import app.flux.stores.entries.factories.TagsStoreFactory.State
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import hydro.models.access.DbQueryImplicits._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class TagsStoreFactory(implicit entityAccess: AppJsEntityAccess) extends EntriesStoreFactory[State] {

  // **************** Public API ****************//
  def get(): Store = get((): Unit)

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    override protected def calculateState() = async {
      val transactionsWithTags: Seq[Transaction] =
        await(
          entityAccess
            .newQuery[Transaction]()
            .filter(ModelFields.Transaction.tags !== Seq())
            .data()
        )

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
