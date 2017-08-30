package flux.react.app

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.I18n
import common.LoggingUtils.logExceptions
import common.time.Clock
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import flux.react.uielements
import flux.stores.entries.AllEntriesStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.EntityAccess
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager

import scala.collection.immutable.Seq

private[app] final class Menu(implicit entriesStoreFactory: AllEntriesStoreFactory,
                              entityAccess: EntityAccess,
                              clock: Clock,
                              accountingConfig: Config,
                              exchangeRateManager: ExchangeRateManager,
                              i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private type State = Unit
  private class Backend(val $ : BackendScope[Props, State]) {
    val searchInputRef = uielements.input.TextInput.ref()

    def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router

      <.ul(
        ^.className := "nav",
        <.li(
          ^.className := "sidebar-search",
          <.form(
            <.div(
              ^.className := "input-group custom-search-form",
              uielements.input
                .TextInput(ref = searchInputRef, name = "query", placeholder = i18n("facto.search")),
              <.span(
                ^.className := "input-group-btn",
                <.button(
                  ^.className := "btn btn-default",
                  ^.tpe := "submit",
                  ^.onClick ==> { (e: ReactEventFromInput) =>
                    LogExceptionsCallback {
                      e.preventDefault()

                      props.router.setPage(Page.Everything)
                    }
                  },
                  <.i(^.className := "fa fa-search")
                )
              )
            ))
        ),
        <.li(
          props.router.anchorWithHrefTo(Page.Everything)(
            <.i(^.className := "icon-list"),
            i18n("facto.everything.html")),
          props.router
            .anchorWithHrefTo(Page.CashFlow)(<.i(^.className := "icon-money"), i18n("facto.cash-flow.html")),
          props.router.anchorWithHrefTo(Page.Liquidation)(
            <.i(^.className := "icon-balance-scale"),
            i18n("facto.liquidation.html")),
          props.router.anchorWithHrefTo(Page.Endowments)(
            <.i(^.className := "icon-crown"),
            i18n("facto.endowments.html")),
          props.router
            .anchorWithHrefTo(Page.Everything)(<.i(^.className := "icon-table"), i18n("facto.summary.html"))
        ),
        <.li(
          props.router.anchorWithHrefTo(Page.NewTransactionGroup())(
            <.i(^.className := "icon-new-empty"),
            i18n("facto.new-entry.html")),
          props.router.anchorWithHrefTo(Page.Everything)(
            <.i(^.className := "icon-template"),
            i18n("facto.templates.html"))
        )
      )
    }
  }

  private case class Props(router: RouterContext)
}
