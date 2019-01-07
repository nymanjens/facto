package app.flux.react.app

import hydro.common.I18n
import app.common.money.ExchangeRateManager
import app.flux.router.AppPages
import hydro.flux.router.StandardPages
import app.flux.stores.entries.factories.AllEntriesStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.user.User
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.common.CollectionUtils
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.uielements.HalfPanel.getClass
import hydro.flux.react.uielements.SbadminMenu
import hydro.flux.react.uielements.SbadminMenu.MenuItem
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[app] final class Menu(implicit entriesStoreFactory: AllEntriesStoreFactory,
                              entityAccess: AppJsEntityAccess,
                              user: User,
                              clock: Clock,
                              accountingConfig: Config,
                              exchangeRateManager: ExchangeRateManager,
                              i18n: I18n,
                              sbadminMenu: SbadminMenu) {

  // **************** API ****************//
  def apply(implicit router: RouterContext): VdomElement = {
    sbadminMenu(
      menuItems = Seq(
        Seq(
          MenuItem(
            i18n("app.everything.html"),
            AppPages.Everything,
            shortcuts = Seq("shift+alt+e", "shift+alt+a")),
          MenuItem(i18n("app.cash-flow.html"), AppPages.CashFlow, shortcuts = Seq("shift+alt+c")),
          MenuItem(
            i18n("app.liquidation.html"),
            AppPages.Liquidation,
            shortcuts = Seq("shift+alt+l", "shift+alt+v")),
          MenuItem(i18n("app.endowments.html"), AppPages.Endowments, shortcuts = Seq("shift+alt+d")),
          MenuItem(i18n("app.summary.html"), AppPages.Summary, shortcuts = Seq("shift+alt+s"))
        ),
        Seq(
          MenuItem(
            i18n("app.templates.html"),
            AppPages.TemplateList,
            shortcuts = Seq("shift+alt+t", "shift+alt+j")),
          MenuItem(i18n("app.new-entry.html"), AppPages.NewTransactionGroup(), shortcuts = Seq("shift+alt+n"))
        ),
        for (template <- newEntryTemplates)
          yield
            MenuItem(template.name, AppPages.NewFromTemplate(template), iconClass = Some(template.iconClass))
      ),
      enableSearch = true,
      router = router
    )
  }

  private def newEntryTemplates(implicit router: RouterContext): Seq[Template] = {
    def templatesForPlacement(placement: Template.Placement): Seq[Template] =
      accountingConfig.templatesToShowFor(placement, user)

    router.currentPage match {
      case AppPages.Everything            => templatesForPlacement(Template.Placement.EverythingView)
      case AppPages.CashFlow              => templatesForPlacement(Template.Placement.CashFlowView)
      case AppPages.Liquidation           => templatesForPlacement(Template.Placement.LiquidationView)
      case AppPages.Endowments            => templatesForPlacement(Template.Placement.EndowmentsView)
      case AppPages.Summary               => templatesForPlacement(Template.Placement.SummaryView)
      case _: StandardPages.Search        => templatesForPlacement(Template.Placement.SearchView)
      case page: AppPages.NewFromTemplate => Seq(accountingConfig.templateWithCode(page.templateCode))
      case _                              => Seq()
    }
  }
}
