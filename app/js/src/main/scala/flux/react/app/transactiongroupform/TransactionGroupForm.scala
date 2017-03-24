package flux.react.app.transactiongroupform

import common.Formatting._
import common.{I18n, LoggingUtils}
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

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](State(numberOfTransactionPanels = 2))
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(): ReactElement = {
    component(Props())
  }

  // **************** Private inner types ****************//
  private case class State(numberOfTransactionPanels: Int) {
    def plusPanel(): State = copy(numberOfTransactionPanels = numberOfTransactionPanels + 1)
    def minusPanel(): State = copy(numberOfTransactionPanels = numberOfTransactionPanels + 1)
  }

  private case class Props()

  private final class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      <.div(
        ^.className := "transaction-group-form",
        for (i <- 0 until state.numberOfTransactionPanels) yield {
          TransactionPanel()
        },
        AddTransactionPanel(onClick = addTransactionPanel())
      )
    }

    private def addTransactionPanel(): Callback = Callback {
      LoggingUtils.logExceptions {
        $.modState(_.plusPanel()).runNow()
      }
    }
  }
}
