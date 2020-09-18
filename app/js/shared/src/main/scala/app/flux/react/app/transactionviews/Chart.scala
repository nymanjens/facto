package app.flux.react.app.transactionviews

import app.common.money.ExchangeRateManager
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.HydroReactComponent
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class Chart(implicit
    summaryTable: SummaryTable,
    entityAccess: AppJsEntityAccess,
    user: User,
    clock: Clock,
    accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n,
    pageHeader: PageHeader,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(stringifiedChartSpecs: String, router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Private inner types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      query: String = accountingConfig.accountOf(user) match {
        case Some(account) if account.longName.contains(" ") => s"beneficiary:'${account.longName}'"
        case Some(account)                                   => s"beneficiary:${account.longName}"
        case None                                            => ""
      }
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    private val queryInputRef = TextInput.ref()

    override def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      <.span(
        pageHeader.withExtension(router.currentPage)(
          Bootstrap.FormInline()(
            ^.className := "summary-query-filter",
            Bootstrap.InputGroup(
              TextInput(
                ref = queryInputRef,
                name = "query",
                placeholder = i18n("app.example-query"),
                classes = Seq("form-control"),
              ),
              Bootstrap.InputGroupButton(
                Bootstrap.Button(tpe = "submit")(
                  ^.onClick ==> { (e: ReactEventFromInput) =>
                    LogExceptionsCallback {
                      e.preventDefault()
                      $.modState(_.copy(query = queryInputRef().value getOrElse "")).runNow()
                    }
                  },
                  Bootstrap.FontAwesomeIcon("search"),
                )
              ),
            ),
          )
        ),
        <.div(
          "Hello world!"
        ),
      )
    }
  }
}
