package flux.react.app.transactionviews

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.money.ExchangeRateManager
import common.time.Clock
import flux.react.router.RouterContext
import flux.react.uielements
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.config.Config
import models.EntityAccess
import models.user.User

import scala.collection.immutable.Seq

final class Summary(implicit summaryTable: SummaryTable,
                    entityAccess: EntityAccess,
                    user: User,
                    clock: Clock,
                    accountingConfig: Config,
                    exchangeRateManager: ExchangeRateManager,
                    i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(
      State(
        includeUnrelatedAccounts = false,
        query = "",
        yearLowerBound = clock.now.getYear - 1,
        expandedYear = clock.now.getYear))
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(includeUnrelatedAccounts: Boolean,
                           query: String,
                           yearLowerBound: Int,
                           expandedYear: Int)

  private class Backend(val $ : BackendScope[Props, State]) {
    val queryInputRef = uielements.input.TextInput.ref()

    def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      <.span(
        uielements.PageHeader.withExtension(router.currentPage)(
          <.form(
            ^.className := "form-inline summary-query-filter",
            <.div(
              ^.className := "input-group",
              uielements.input
                .TextInput(
                  ref = queryInputRef,
                  name = "query",
                  placeholder = i18n("facto.example-query"),
                  classes = Seq("form-control")),
              <.span(
                ^.className := "input-group-btn",
                <.button(
                  ^.className := "btn btn-default",
                  ^.tpe := "submit",
                  ^.onClick ==> { (e: ReactEventFromInput) =>
                    LogExceptionsCallback {
                      e.preventDefault()
                      $.modState(_.copy(query = queryInputRef().value getOrElse "")).runNow()
                    }
                  },
                  <.i(^.className := "fa fa-search")
                )
              )
            )
          )), {
          for {
            account <- accountingConfig.personallySortedAccounts
            if state.includeUnrelatedAccounts || account.isMineOrCommon
          } yield {
            uielements.Panel(account.longName, key = account.code) {
              summaryTable(
                account = account,
                query = state.query,
                yearLowerBound = state.yearLowerBound,
                expandedYear = state.expandedYear,
                onShowHiddenYears = $.modState(_.copy(yearLowerBound = Int.MinValue)),
                onSetExpandedYear = year => $.modState(_.copy(expandedYear = year))
              )
            }
          }
        }.toVdomArray,
        // includeUnrelatedAccounts toggle button
        <.a(
          ^.className := "btn btn-info btn-lg btn-block",
          ^.onClick --> $.modState(s => s.copy(includeUnrelatedAccounts = !s.includeUnrelatedAccounts)),
          if (state.includeUnrelatedAccounts) i18n("facto.hide-other-accounts")
          else i18n("facto.show-other-accounts")
        )
      )
    }
  }
}
