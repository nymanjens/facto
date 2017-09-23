package flux.stores.entries

import scala2js.Converters._
import common.GuavaReplacement.ImmutableSetMultimap
import common.accounting.Tag
import flux.stores.entries.TagsStoreFactory.State
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Keys

final class TagsStoreFactory(implicit database: RemoteDatabaseProxy) extends EntriesStoreFactory[State] {

  // **************** Public API ****************//
  def get(): Store = get((): Unit)

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    override protected def calculateState() = {
      val transactionsWithTags: Seq[Transaction] =
        database.newQuery[Transaction]().filterNot(Keys.Transaction.tagsString, "").data()

      val tagToTransactionIdsBuilder = ImmutableSetMultimap.builder[Tag, Long]()
      for {
        transaction <- transactionsWithTags
        tag <- transaction.tags
      } tagToTransactionIdsBuilder.put(tag, transaction.id)
      State(tagToTransactionIdsBuilder.build())
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) = {
      transaction.tags.nonEmpty
    }
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
  }

  /* override */
  protected type Input = Unit
}

object TagsStoreFactory {
  case class State(tagToTransactionIds: ImmutableSetMultimap[Tag, Long])
}
