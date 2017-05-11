package flux.react.app.transactiongroupform

import flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.accounting.money.{Currency, ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq

final class TransactionGroupForm(implicit i18n: I18n,
                                 exchangeRateManager: ExchangeRateManager,
                                 transactionPanel: TransactionPanel,
                                 addTransactionPanel: AddTransactionPanel,
                                 totalFlowRestrictionInput: TotalFlowRestrictionInput) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState[State](
    State(
      // TODO: Update these when updating an existing TransactionGroup.
      panelIndices = Seq(0, 1),
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
  private case class State(panelIndices: Seq[Int],
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
                <.span(
                  ^.className := "total-flow-form form-inline",
                  <.div(^.className := "input-group",
                    <.span(
                      ^.className := "input-group-addon",
                      i18n("facto.total") + ":"
                    ),
                    <.span(
                      ^.className := "input-group-addon",
                      <.i(^.className := Currency.default.iconClass)
                    ),
                    <.input(
                      ^.tpe := "text",
                      ^.className := "form-control",
                      ^.autoComplete := "off",
                      ^.value := state.totalFlow.formatFloat,
                      ^.disabled := state.totalFlowRestriction != TotalFlowRestriction.ChooseTotal,
                      ^.onChange --> Callback((): Unit)
                    )
                  )
                ),
                totalFlowRestrictionInput(defaultValue = TotalFlowRestriction.AnyTotal, onChange = updateTotalFlowRestriction)
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
                    if (lastPanel && state.totalFlowRestriction.userSetsTotal) Some((state.totalFlow - state.totalFlowExceptLast).cents) else None,
                  defaultPanel = if (firstPanel) None else Some(panelRef(panelIndex = state.panelIndices.head)($)),
                  closeButtonCallback = if (firstPanel) None else Some(removeTransactionPanel(panelIndex)),
                  onFormChange = this.onFormChange
                )
              },
              addTransactionPanel(onClick = addTransactionPanelCallback())
            ),
            <.div(
              ^.className := "form-group",
              <.div(
                ^.className := "col-sm-offset-2 col-sm-10",
                <.button(
                  ^.tpe := "submit",
                  ^.className := "btn btn-default",
                  i18n("facto.ok"))
              )
            )
          )
        )
      )
    }

    private def addTransactionPanelCallback(): Callback = LogExceptionsCallback {
      $.modState(_.plusPanel()).runNow()
    }

    private def removeTransactionPanel(index: Int): Callback = LogExceptionsCallback {
      $.modState(_.minusPanelIndex(index)).runNow()
    }

    private def updateTotalFlowRestriction(totalFlowRestriction: TotalFlowRestriction): Unit = {
      $.modState(_.copy(totalFlowRestriction = totalFlowRestriction)).runNow()
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
