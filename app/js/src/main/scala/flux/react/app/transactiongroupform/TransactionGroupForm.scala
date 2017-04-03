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
import models.{EntityAccess, User}
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager

import scala.collection.immutable.Seq

final class TransactionGroupForm(implicit accountingConfig: Config,
                                 clock: Clock,
                                 exchangeRateManager: ExchangeRateManager,
                                 i18n: I18n,
                                 user: User,
                                 entityAccess: EntityAccess) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](State(panelIndices = Seq(0, 1)))
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(): ReactElement = {
    component(Props())
  }

  // **************** Private helper methods ****************//
  private def panelRef(panelIndex: Int): TransactionPanel.Reference = TransactionPanel.ref(s"panel_$panelIndex")

  // **************** Private inner types ****************//
  private case class State(panelIndices: Seq[Int]) {
    def plusPanel(): State = copy(panelIndices = panelIndices :+ panelIndices.max + 1)
    def minusPanelIndex(index: Int): State = copy(panelIndices = panelIndices.filter(_ != index))
  }

  private case class Props()

  private final class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      <.div(
        ^.className := "transaction-group-form",
        for ((panelIndex, i) <- state.panelIndices.zipWithIndex) yield {
          val firstPanel = state.panelIndices.head
          TransactionPanel(
            key = panelIndex,
            ref = panelRef(panelIndex),
            title = i18n("facto.transaction") + " " + (i + 1),
            defaultPanel = if (panelIndex == firstPanel) None else Some(panelRef(panelIndex = firstPanel)($)),
            closeButtonCallback = if (panelIndex == firstPanel) None else Some(removeTransactionPanel(panelIndex))
          )
        },
        AddTransactionPanel(onClick = addTransactionPanel())
      )
    }

    private def addTransactionPanel(): Callback = Callback {
      LoggingUtils.logExceptions {
        $.modState(_.plusPanel()).runNow()
      }
    }

    private def removeTransactionPanel(index: Int): Callback = Callback {
      LoggingUtils.logExceptions {
        $.modState(_.minusPanelIndex(index)).runNow()
      }
    }
  }
}
