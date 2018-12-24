package flux.react.app

import common.I18n
import common.LoggingUtils.LogExceptionsCallback
import common.LoggingUtils.logExceptions
import common.money.ExchangeRateManager
import common.time.Clock
import flux.react.ReactVdomUtils.<<
import flux.react.ReactVdomUtils.^^
import flux.react.ReactVdomUtils.^^
import flux.react.common.HydroReactComponent
import flux.react.router.Page
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.entries.factories.AllEntriesStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import jsfacades.Mousetrap
import models.access.EntityAccess
import models.accounting.config.Config
import models.accounting.config.Template
import models.user.User

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
    val queryInputRef = uielements.input.TextInput.ref()

    override def willMount(props: Props, state: State): Callback = configureKeyboardShortcuts(props.router)

    override def didMount(props: Props, state: State): Callback = LogExceptionsCallback {
      props.router.currentPage match {
        case page: Page.Search => {
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
              uielements.input
                .TextInput(
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
                        case Some(query) => props.router.setPage(Page.Search(query))
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
          menuItem(i18n("app.everything.html"), Page.Everything),
          menuItem(i18n("app.cash-flow.html"), Page.CashFlow),
          menuItem(i18n("app.liquidation.html"), Page.Liquidation),
          menuItem(i18n("app.endowments.html"), Page.Endowments),
          menuItem(i18n("app.summary.html"), Page.Summary)
        ),
        <.li(
          menuItem(i18n("app.templates.html"), Page.TemplateList),
          menuItem(i18n("app.new-entry.html"), Page.NewTransactionGroup())
        ),
        <<.ifThen(newEntryTemplates.nonEmpty) {
          <.li({
            for (template <- newEntryTemplates)
              yield menuItem(template.name, Page.NewFromTemplate(template), iconClass = template.iconClass)
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

      bindToPage("shift+alt+e", Page.Everything)
      bindToPage("shift+alt+a", Page.Everything)
      bindToPage("shift+alt+c", Page.CashFlow)
      bindToPage("shift+alt+l", Page.Liquidation)
      bindToPage("shift+alt+v", Page.Liquidation)
      bindToPage("shift+alt+d", Page.Endowments)
      bindToPage("shift+alt+s", Page.Summary)
      bindToPage("shift+alt+t", Page.TemplateList)
      bindToPage("shift+alt+j", Page.TemplateList)
      bindToPage("shift+alt+n", Page.NewTransactionGroup())

      bind("shift+alt+f", () => queryInputRef().focus())
    }

    private def newEntryTemplates(implicit router: RouterContext): Seq[Template] = {
      def templatesForPlacement(placement: Template.Placement): Seq[Template] =
        accountingConfig.templatesToShowFor(placement, user)

      router.currentPage match {
        case Page.Everything            => templatesForPlacement(Template.Placement.EverythingView)
        case Page.CashFlow              => templatesForPlacement(Template.Placement.CashFlowView)
        case Page.Liquidation           => templatesForPlacement(Template.Placement.LiquidationView)
        case Page.Endowments            => templatesForPlacement(Template.Placement.EndowmentsView)
        case Page.Summary               => templatesForPlacement(Template.Placement.SummaryView)
        case _: Page.Search             => templatesForPlacement(Template.Placement.SearchView)
        case page: Page.NewFromTemplate => Seq(accountingConfig.templateWithCode(page.templateCode))
        case _                          => Seq()
      }
    }
  }
}
