package flux.stores.entries

import jsfacades.LokiJs
import jsfacades.LokiJsImplicits._
import models.access.RemoteDatabaseProxy
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class ComplexQueryStoreFactory(implicit database: RemoteDatabaseProxy,
                                     complexQueryFilter: ComplexQueryFilter)
    extends EntriesListStoreFactory[GeneralEntry, ComplexQueryStoreFactory.Query] {

  override protected def createNew(maxNumEntries: Int, query: String) = new Store {
    private val filterFromQuery: LokiJs.Filter[Transaction] = complexQueryFilter.fromQuery(query)

    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(filterFromQuery)
          .sort(LokiJs.Sorting.descBy(Keys.Transaction.createdDate).thenDescBy(Keys.id))
          .limit(3 * maxNumEntries)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State
        .withImpactingIdsInEntries(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      LokiJs.ResultSet.fake(Seq(transaction)).filter(filterFromQuery).count() > 0
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
  }

  def get(query: String, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = query))
}

object ComplexQueryStoreFactory {
  type Query = String
}
