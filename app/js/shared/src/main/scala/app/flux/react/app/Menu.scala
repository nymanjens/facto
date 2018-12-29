package app.flux.react.app

import common.I18n
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import common.money.ExchangeRateManager
import hydro.common.time.Clock
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import app.flux.router.AppPages
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import app.flux.stores.entries.factories.AllEntriesStoreFactory
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.input.TextInput
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.jsfacades.Mousetrap
import app.models.access.EntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.user.User

import scala.collection.immutable.Seq

private[app] final class Menu(implicit entriesStoreFactory: AllEntriesStoreFactory,
                              entityAccess: EntityAccess,
                              user: User,
                              clock: Clock,
                              accountingConfig: Config,
                              exchangeRateManager: ExchangeRateManager,
                              i18n: I18n)
    extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with WillMount
      with DidMount
      with WillReceiveProps {
    val queryInputRef = TextInput.ref()

    override def willMount(props: Props, state: State): Callback = configureKeyboardShortcuts(props.router)

    override def didMount(props: Props, state: State): Callback = LogExceptionsCallback {
      props.router.currentPage match {
        case page: AppPages.Search => {
          queryInputRef().setValue(page.query)
        }
        case _ =>
      }
    }

    override def willReceiveProps(currentProps: Props, nextProps: Props, state: State): Callback =
      configureKeyboardShortcuts(nextProps.router)

    override def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      def menuItem(label: String, page: Page, iconClass: String = null): VdomElement =
        router
          .anchorWithHrefTo(page)(
            ^^.ifThen(page.getClass == props.router.currentPage.getClass) { ^.className := "active" },
            ^.key := label,
            <.i(^.className := Option(iconClass) getOrElse page.iconClass),
            " ",
            <.span(^.dangerouslySetInnerHtml := label)
          )

      <.ul(
        ^.className := "nav",
        ^.id := "side-menu",
        <.li(
          ^.className := "sidebar-search",
          <.form(
            <.div(
              ^.className := "input-group custom-search-form",
              TextInput(
                ref = queryInputRef,
                name = "query",
                placeholder = i18n("app.search"),
                classes = Seq("form-control")),
              <.span(
                ^.className := "input-group-btn",
                <.button(
                  ^.className := "btn btn-default",
                  ^.tpe := "submit",
                  ^.onClick ==> { (e: ReactEventFromInput) =>
                    LogExceptionsCallback {
                      e.preventDefault()

                      queryInputRef().value match {
                        case Some(query) => props.router.setPage(AppPages.Search(query))
                        case None        =>
                      }
                    }
                  },
                  <.i(^.className := "fa fa-search")
                )
              )
            ))
        ),
        <.li(
          menuItem(i18n("app.everything.html"), AppPages.Everything),
          menuItem(i18n("app.cash-flow.html"), AppPages.CashFlow),
          menuItem(i18n("app.liquidation.html"), AppPages.Liquidation),
          menuItem(i18n("app.endowments.html"), AppPages.Endowments),
          menuItem(i18n("app.summary.html"), AppPages.Summary)
        ),
        <.li(
          menuItem(i18n("app.templates.html"), AppPages.TemplateList),
          menuItem(i18n("app.new-entry.html"), AppPages.NewTransactionGroup())
        ),
        <<.ifThen(newEntryTemplates.nonEmpty) {
          <.li({
            for (template <- newEntryTemplates)
              yield
                menuItem(template.name, AppPages.NewFromTemplate(template), iconClass = template.iconClass)
          }.toVdomArray)
        }
      )
    }

    def configureKeyboardShortcuts(implicit router: RouterContext): Callback = LogExceptionsCallback {
      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bindGlobal(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }
      def bindToPage(shortcut: String, page: Page): Unit =
        bind(shortcut, () => {
          router.setPage(page)
        })

      bindToPage("shift+alt+e", AppPages.Everything)
      bindToPage("shift+alt+a", AppPages.Everything)
      bindToPage("shift+alt+c", AppPages.CashFlow)
      bindToPage("shift+alt+l", AppPages.Liquidation)
      bindToPage("shift+alt+v", AppPages.Liquidation)
      bindToPage("shift+alt+d", AppPages.Endowments)
      bindToPage("shift+alt+s", AppPages.Summary)
      bindToPage("shift+alt+t", AppPages.TemplateList)
      bindToPage("shift+alt+j", AppPages.TemplateList)
      bindToPage("shift+alt+n", AppPages.NewTransactionGroup())

      bind("shift+alt+f", () => queryInputRef().focus())
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
        case _: AppPages.Search             => templatesForPlacement(Template.Placement.SearchView)
        case page: AppPages.NewFromTemplate => Seq(accountingConfig.templateWithCode(page.templateCode))
        case _                              => Seq()
      }
    }
  }
}
