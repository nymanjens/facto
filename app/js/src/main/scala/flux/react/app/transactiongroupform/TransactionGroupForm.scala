package flux.react.app.transactiongroupform

import common.Formatting._
import common.I18n
import common.time.Clock
import flux.react.uielements
import flux.react.uielements.EntriesListTable.NumEntriesStrategy
import flux.stores.AllEntriesStoreFactory
import flux.stores.entries.GeneralEntry
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.EntityAccess
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager

import scala.collection.immutable.Seq

final class TransactionGroupForm(implicit accountingConfig: Config,
                                 clock: Clock,
                                 exchangeRateManager: ExchangeRateManager,
                                 i18n: I18n) {

  private val component = ReactComponentB[Unit](getClass.getSimpleName)
    .renderPS((_, props, state) =>
      <.div(
        ^.className := "transaction-group-form",
        TransactionPanel(),
        AddTransactionPanel(onClick = Callback())
      )
    ).build

  def apply(): ReactElement = {
    component()
  }
}
