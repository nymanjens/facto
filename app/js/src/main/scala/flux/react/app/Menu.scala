package flux.react.app

import common.I18n
import common.time.Clock
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.router.Page
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.stores.entries.AllEntriesStoreFactory
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

  def apply(currentPage: Page, router: RouterCtl[Page]): VdomElement = {
    component(Menu.Props(currentPage, router))
  }

  private val component = ScalaComponent
    .builder[Menu.Props](getClass.getSimpleName)
    .render_P { props =>
      <.div(
        <.b("Menu:"),
        (for ((item, i) <- Menu.menuItems.zipWithIndex) yield {
          <.span(
            ^.key := i,
            ^^.ifThen(props.currentPage == item.page) { ^.className := "active" },
            props.router.link(item.page)(
              <.i(^^.classes(item.iconClass)),
              " ",
              i18n(item.labelKey)
            ),
            " - "
          )
        }).toVdomArray
      )
    }
    .build
}

private[app] object Menu {

  private val menuItems = Seq(
    MenuItem("Everything", "fa fa-money", Page.Everything),
    MenuItem("CashFlow", "icon-money", Page.CashFlow),
    MenuItem("Liquidation", "icon-balance-scale", Page.Liquidation),
    MenuItem("Endowments", "icon-crown", Page.Endowments),
    MenuItem("New", "icon-new-empty", Page.NewTransactionGroup())
  )

  private case class Props(currentPage: Page, router: RouterCtl[Page])
  private case class MenuItem(labelKey: String, iconClass: String, page: Page)
}
