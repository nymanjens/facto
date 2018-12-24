package flux.stores.entries.factories

import flux.stores.entries.ComplexQueryFilter
import flux.stores.entries.GeneralEntry
import models.access.DbQuery
import models.access.JsEntityAccess
import models.accounting.BalanceCheck
import models.accounting.Transaction

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

final class ComplexQueryStoreFactory(implicit entityAccess: JsEntityAccess,
                                     complexQueryFilter: ComplexQueryFilter)
    extends EntriesListStoreFactory[GeneralEntry, ComplexQueryStoreFactory.Query] {

  override protected def createNew(maxNumEntries: Int, query: String) = new Store {
    private val filterFromQuery: DbQuery.Filter[Transaction] = complexQueryFilter.fromQuery(query)

    override protected def calculateState() = async {
      val transactions: Seq[Transaction] =
        await(
          entityAccess
            .newQuery[Transaction]()
            .filter(filterFromQuery)
            .sort(DbQuery.Sorting.Transaction.deterministicallyByCreateDate.reversed)
            .limit(3 * maxNumEntries)
            .data()).reverse

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
