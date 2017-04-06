package flux.react.app.transactiongroupform

import common.{I18n, LoggingUtils}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

final class TransactionGroupForm(implicit i18n: I18n,
                                 transactionPanel: TransactionPanel,
                                 addTransactionPanel: AddTransactionPanel) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](State(panelIndices = Seq(0, 1)))
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(): ReactElement = {
    component(Props())
  }

  // **************** Private helper methods ****************//
  private def panelRef(panelIndex: Int): transactionPanel.Reference = transactionPanel.ref(s"panel_$panelIndex")

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
          transactionPanel(
            key = panelIndex,
            ref = panelRef(panelIndex),
            title = i18n("facto.transaction") + " " + (i + 1),
            defaultPanel = if (panelIndex == firstPanel) None else Some(panelRef(panelIndex = firstPanel)($)),
            closeButtonCallback = if (panelIndex == firstPanel) None else Some(removeTransactionPanel(panelIndex))
          )
        },
        addTransactionPanel(onClick = addTransactionPanelCallback())
      )
    }

    private def addTransactionPanelCallback(): Callback = Callback {
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
