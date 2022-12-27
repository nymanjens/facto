package app.flux.react.app

import hydro.jsfacades.Mousetrap
import app.common.money.CurrencyValueManager
import app.flux.router.AppPages
import app.flux.stores.entries.factories.AllEntriesStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.user.User
import hydro.common.I18n
import hydro.common.time.Clock
import hydro.flux.react.uielements.SbadminMenu
import hydro.flux.react.uielements.SbadminMenu.MenuItem
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[app] final class Menu(implicit
    accountingConfig: Config,
    user: User,
    entityAccess: AppJsEntityAccess,
    i18n: I18n,
    sbadminMenu: SbadminMenu,
) {

  // **************** API ****************//
  def apply()(implicit router: RouterContext): VdomElement = {
    sbadminMenu(
      menuItems = Seq(
        Seq(
          MenuItem(
            i18n("app.everything.html"),
            AppPages.Everything,
            shortcuts = Seq("shift+alt+e", "shift+alt+a"),
          ),
          MenuItem(i18n("app.cash-flow.html"), AppPages.CashFlow, shortcuts = Seq("shift+alt+c")),
          MenuItem(
            i18n("app.liquidation.html"),
            AppPages.Liquidation,
            shortcuts = Seq("shift+alt+l", "shift+alt+v"),
          ),
          MenuItem(i18n("app.endowments.html"), AppPages.Endowments, shortcuts = Seq("shift+alt+d")),
          MenuItem(i18n("app.summary.html"), AppPages.Summary, shortcuts = Seq("shift+alt+s")),
          MenuItem(i18n("app.chart.html"), AppPages.Chart.firstPredefined, shortcuts = Seq("shift+alt+r")),
        ),
        Seq(
          MenuItem(
            i18n("app.templates.html"),
            AppPages.TemplateList,
            shortcuts = Seq("shift+alt+t", "shift+alt+j"),
          ),
          MenuItem(i18n("app.new-entry.html"), AppPages.NewTransactionGroup(), shortcuts = Seq("shift+alt+n")),
        ),
        for (template <- newEntryTemplates)
          yield MenuItem(
            template.name,
            AppPages.NewFromTemplate(template),
            iconClass = Some(template.iconClass),
          ),
      ),
      enableSearch = true,
      router = router,
      configureAdditionalKeyboardShortcuts = () => configureAdditionalKeyboardShortcuts(),
    )
  }
  private def configureAdditionalKeyboardShortcuts()(implicit router: RouterContext): Unit = {
    def bind(shortcut: String, runnable: () => Unit): Unit = {
      Mousetrap.bind(
        shortcut,
        e => {
          e.preventDefault()
          runnable()
        },
      )
    }
    def bindGlobal(shortcut: String, runnable: () => Unit): Unit = {
      Mousetrap.bindGlobal(
        shortcut,
        e => {
          e.preventDefault()
          runnable()
        },
      )
    }

    bindGlobal(
      "escape",
      () => {
        router.currentPage match {
          case page: AppPages.PopupEditorPage => router.setPage(page.parentPage)
        }
      },
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
