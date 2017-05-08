package flux.react.app.transactiongroupform

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.accounting.money.{ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq

final class TransactionGroupForm(implicit i18n: I18n,
                                 exchangeRateManager: ExchangeRateManager,
                                 transactionPanel: TransactionPanel,
                                 addTransactionPanel: AddTransactionPanel) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](
    State(
      // TODO: Update these when updating an existing TransactionGroup.
      panelIndices = Seq(0, 1),
      totalFlow = ReferenceMoney(0),
      totalFlowExceptLast = ReferenceMoney(0)))
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(): ReactElement = {
    component(Props())
  }

  // **************** Private helper methods ****************//
  private def panelRef(panelIndex: Int): transactionPanel.Reference = transactionPanel.ref(s"panel_$panelIndex")

  // **************** Private inner types ****************//
  private case class State(panelIndices: Seq[Int],
                           totalFlow: ReferenceMoney,
                           totalFlowExceptLast: ReferenceMoney) {
    def plusPanel(): State = copy(panelIndices = panelIndices :+ panelIndices.max + 1)
    def minusPanelIndex(index: Int): State = copy(panelIndices = panelIndices.filter(_ != index))
  }

  private case class Props()

  private final class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = logExceptions {
      <.div(
        <.div(
          state.totalFlow.toString,
          state.totalFlowExceptLast.toString
        ),
        <.div(
          ^.className := "transaction-group-form",
          for ((panelIndex, i) <- state.panelIndices.zipWithIndex) yield {
            val firstPanel = state.panelIndices.head
            transactionPanel(
              key = panelIndex,
              ref = panelRef(panelIndex),
              title = i18n("facto.transaction") + " " + (i + 1),
              defaultPanel = if (panelIndex == firstPanel) None else Some(panelRef(panelIndex = firstPanel)($)),
              closeButtonCallback = if (panelIndex == firstPanel) None else Some(removeTransactionPanel(panelIndex)),
              onFormChange = this.onFormChange
            )
          },
          addTransactionPanel(onClick = addTransactionPanelCallback())
        )
      )
    }

    private def addTransactionPanelCallback(): Callback = LogExceptionsCallback {
      $.modState(_.plusPanel()).runNow()
    }

    private def removeTransactionPanel(index: Int): Callback = LogExceptionsCallback {
      $.modState(_.minusPanelIndex(index)).runNow()
    }

    private def onFormChange(): Unit = {
      val state = $.state.runNow()
      val flows = for (panelIndex <- state.panelIndices) yield {
        val datedMoney = panelRef(panelIndex)($).flowValueOrDefault
        datedMoney.exchangedForReferenceCurrency
      }

      $.modState(_.copy(
        totalFlow = flows.sum,
        totalFlowExceptLast = flows.dropRight(1).sum)).runNow()
    }
  }
}
