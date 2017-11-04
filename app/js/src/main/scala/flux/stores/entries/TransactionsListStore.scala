package flux.stores.entries
import flux.stores.entries.EntriesListStoreFactory.State
import models.access.RemoteDatabaseProxy
import models.accounting.BalanceCheck

private[entries] abstract class TransactionsListStore[Entry <: GroupedTransactions](
    implicit database: RemoteDatabaseProxy)
    extends EntriesStore[EntriesListStoreFactory.State[Entry]] {

  override final protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck,
                                                              state: State[Entry]) =
    false
}
