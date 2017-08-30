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

  // **************** Private helper methods ****************//
  private def menuItems(implicit router: RouterContext) = Seq(
    MenuItem("Everything", "fa fa-money", Page.Everything),
    MenuItem("CashFlow", "icon-money", Page.CashFlow),
    MenuItem("Liquidation", "icon-balance-scale", Page.Liquidation),
    MenuItem("Endowments", "icon-crown", Page.Endowments),
    MenuItem("New", "icon-new-empty", Page.NewTransactionGroup())
  )

  // **************** Private inner types ****************//
  private type State = Unit
  private class Backend(val $ : BackendScope[Props, State]) {
    val searchInputRef = uielements.input.TextInput.ref()

    def render(props: Props, state: State) = logExceptions {
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
          (for ((item, i) <- menuItems(props.router).zipWithIndex) yield {
            props.router
              .anchorWithHrefTo(item.page)(^.key := i, <.i(^.className := item.iconClass), item.label)
          }).toVdomArray
        )
      )
    }
  }

  private case class Props(router: RouterContext)
  private case class MenuItem(label: String, iconClass: String, page: Page)
}
