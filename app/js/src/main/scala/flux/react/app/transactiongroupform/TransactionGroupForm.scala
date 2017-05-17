package flux.react.app.transactiongroupform

import common.{I18n, SinglePendingTaskQueue}
import common.LoggingUtils.{LogExceptionsCallback, LogExceptionsFuture, logExceptions}
import flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.accounting.money.{Currency, ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class TransactionGroupForm(implicit i18n: I18n,
                                 exchangeRateManager: ExchangeRateManager,
                                 transactionPanel: TransactionPanel,
                                 addTransactionPanel: AddTransactionPanel,
                                 totalFlowInput: TotalFlowInput,
                                 totalFlowRestrictionInput: TotalFlowRestrictionInput) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](
    State(
      // TODO: Update these when updating an existing TransactionGroup.
      panelIndices = Seq(0, 1),
      foreignCurrency = None,
      totalFlowRestriction = TotalFlowRestriction.AnyTotal,
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
  /**
    * @param foreignCurrency Any foreign currency of any of the selected money reservoirs. If there are multiple,
    *                        this can by any of these.
    */
  private case class State(panelIndices: Seq[Int],
                           showErrorMessages: Boolean = false,
                           foreignCurrency: Option[Currency],
                           totalFlowRestriction: TotalFlowRestriction,
                           totalFlow: ReferenceMoney,
                           totalFlowExceptLast: ReferenceMoney) {
    def plusPanel(): State = copy(panelIndices = panelIndices :+ panelIndices.max + 1)
    def minusPanelIndex(index: Int): State = copy(panelIndices = panelIndices.filter(_ != index))
  }

  private case class Props()

  private final class Backend(val $: BackendScope[Props, State]) {

    def render(props: Props, state: State) = logExceptions {
      <.div(
        ^.className := "transaction-group-form",
        <.div(
          ^.className := "row",
          <.div(
            ^.className := "col-lg-12",
            <.h1(
              ^.className := "page-header",
              <.i(^.className := "icon-new-empty"),
              i18n("facto.new-transaction"),
              // TODO: Add delete action here
              <.span(
                ^.className := "total-transaction-flow-box",
                totalFlowInput(
                  forceValue =
                    if (state.totalFlowRestriction == TotalFlowRestriction.ChooseTotal) None else Some(state.totalFlow),
                  foreignCurrency = state.foreignCurrency,
                  onChange = updateTotalFlow
                ),
                totalFlowRestrictionInput(
                  defaultValue = TotalFlowRestriction.AnyTotal,
                  onChange = updateTotalFlowRestriction
                )
              )
            )
          )
        ),

        // TODO: Add global form errors here
        //for (error <- (transGroupForm.globalErrors) yield {
        //  <.div(^.className := "alert alert-danger",
        //    error.message
        //  )
        //},
        <.form(
          ^.className := "form-horizontal",
          <.div(^.className := "row",
            <.div(
              ^.className := "transaction-group-form",
              for ((panelIndex, i) <- state.panelIndices.zipWithIndex) yield {
                val firstPanel = panelIndex == state.panelIndices.head
                val lastPanel = panelIndex == state.panelIndices.last
                transactionPanel(
                  key = panelIndex,
                  ref = panelRef(panelIndex),
                  title = i18n("facto.transaction") + " " + (i + 1),
                  forceFlowValue =
                    if (lastPanel && state.totalFlowRestriction.userSetsTotal) Some(state.totalFlow - state.totalFlowExceptLast) else None,
                  showErrorMessages = state.showErrorMessages,
                  defaultPanel = if (firstPanel) None else Some(panelRef(panelIndex = state.panelIndices.head)($)),
                  closeButtonCallback = if (firstPanel) None else Some(removeTransactionPanel(panelIndex)),
                  onFormChange = this.onFormChange
                )
              },
              addTransactionPanel(onClick = addTransactionPanelCallback)
            ),
            <.div(
              ^.className := "form-group",
              <.div(
                ^.className := "col-sm-offset-2 col-sm-10",
                <.button(
                  ^.tpe := "submit",
                  ^.className := "btn btn-default",
                  ^.onClick ==> onSubmit,
                  i18n("facto.ok")
                )
              )
            )
          )
        )
      )
    }

    private val addTransactionPanelCallback: Callback = LogExceptionsCallback {
      $.modState(_.plusPanel()).runNow()
      LogExceptionsFuture(onFormChange()) // Make sure the state is updated after this
    }

    private def removeTransactionPanel(index: Int): Callback = LogExceptionsCallback {
      $.modState(_.minusPanelIndex(index)).runNow()
      LogExceptionsFuture(onFormChange()) // Make sure the state is updated after this
    }

    private def updateTotalFlow(totalFlow: ReferenceMoney): Unit = {
      $.modState(_.copy(totalFlow = totalFlow)).runNow()
    }

    private def updateTotalFlowRestriction(totalFlowRestriction: TotalFlowRestriction): Unit = {
      $.modState { state =>
        var newState = state.copy(totalFlowRestriction = totalFlowRestriction)
        if (totalFlowRestriction == TotalFlowRestriction.ZeroSum) {
          newState = newState.copy(totalFlow = ReferenceMoney(0))
        }
        newState
      }.runNow()
    }

    private def onFormChange(): Unit = {
      $.modState { state =>
        val flows = for (panelIndex <- state.panelIndices) yield {
          val datedMoney = panelRef(panelIndex)($).flowValueOrDefault
          datedMoney.exchangedForReferenceCurrency
        }
        val currencies = for (panelIndex <- state.panelIndices) yield {
          panelRef(panelIndex)($).moneyReservoir.valueOrDefault.currency
        }

        var newState = state.copy(
          foreignCurrency = currencies.filter(_.isForeign).headOption,
          totalFlowExceptLast = flows.dropRight(1).sum)
        if (state.totalFlowRestriction == TotalFlowRestriction.AnyTotal) {
          newState = newState.copy(totalFlow = flows.sum)
        }
        newState
      }.runNow()
    }

    private def onSubmit(e: ReactEventI): Callback = LogExceptionsCallback {
      e.preventDefault()

      val state = $.state.runNow()
      println("  onSubmit:")
      for (panelIndex <- state.panelIndices) {
        println("   - " + panelRef(panelIndex)($).data)
      }
      $.modState(_.copy(showErrorMessages = true)).runNow()
    }
  }
}
