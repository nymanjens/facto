package flux.stores.entries

import jsfacades.LokiJs
import models.access.DbQuery
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._
import models.access.Fields

final class ComplexQueryStoreFactory(implicit database: RemoteDatabaseProxy,
                                     complexQueryFilter: ComplexQueryFilter)
    extends EntriesListStoreFactory[GeneralEntry, ComplexQueryStoreFactory.Query] {

  override protected def createNew(maxNumEntries: Int, query: String) = new Store {
    private val filterFromQuery: DbQuery.Filter[Transaction] = complexQueryFilter.fromQuery(query)

    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(filterFromQuery)
          .sort(DbQuery.Sorting.descBy(Fields.Transaction.createdDate).thenDescBy(Fields.id))
          .limit(3 * maxNumEntries)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State
        .withImpactingIdsInEntries(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      filterFromQuery(transaction)
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
  }

  def get(query: String, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = query))
}

object ComplexQueryStoreFactory {
  type Query = String
}
