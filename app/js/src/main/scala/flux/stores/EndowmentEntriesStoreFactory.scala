package flux.stores

import flux.stores.entries.GeneralEntry
import jsfacades.Loki
import models.access.RemoteDatabaseProxy
import models.accounting.Transaction
import models.accounting.config.{Account, Config}
import models.manager.{EntityModification, EntityType}

import scala.collection.immutable.Seq

final class EndowmentEntriesStoreFactory(implicit database: RemoteDatabaseProxy, accountingConfig: Config)
    extends EntriesListStoreFactory[GeneralEntry, Account] {

  override protected def createNew(maxNumEntries: Int, account: Account) = new Store {
    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .find("categoryCode" -> accountingConfig.constants.endowmentCategory.code)
          .find("beneficiaryAccountCode" -> account.code)
          .sort(
            Loki.Sorting
              .by("consumedDate")
              .desc()
              .thenBy("createdDate")
              .desc()
              .thenBy("id")
              .desc())
          .limit(3 * maxNumEntries)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def modificationImpactsState(entityModification: EntityModification,
                                                    state: State): Boolean = {
      entityModification.entityType == EntityType.TransactionType
    }
  }

  def get(account: Account, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = account))
}
