package flux.stores.entries

import jsfacades.LokiJs
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class ComplexQueryStoreFactory(implicit database: RemoteDatabaseProxy,
                                     complexQueryFilter: ComplexQueryFilter)
    extends EntriesListStoreFactory[GeneralEntry, ComplexQueryStoreFactory.Query] {

  override protected def createNew(maxNumEntries: Int, query: String) = new TransactionsListStore[GeneralEntry] {
    private val filter = complexQueryFilter.fromQuery(query)

    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        filter(database.newQuery[Transaction]())
          .sort(LokiJs.Sorting.descBy(Keys.Transaction.createdDate).thenDescBy(Keys.id))
          .limit(3 * maxNumEntries)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      filter(LokiJs.ResultSet.fake(Seq(transaction))).count() > 0
  }

  def get(query: String, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = query))
}

object ComplexQueryStoreFactory {
  type Query = String
}
