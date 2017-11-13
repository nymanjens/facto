package flux.stores.entries

import common.GuavaReplacement.ImmutableSetMultimap
import flux.stores.entries.TagsStoreFactory.State
import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class TagsStoreFactory(implicit database: RemoteDatabaseProxy) extends EntriesStoreFactory[State] {

  // **************** Public API ****************//
  def get(): Store = get((): Unit)

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    override protected def calculateState() = {
      val transactionsWithTags: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(LokiJs.Filter.notEqual(Keys.Transaction.tags, Seq()))
          .data()

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
