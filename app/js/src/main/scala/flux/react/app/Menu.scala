package flux.react.app

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.time.Clock
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import flux.react.uielements
import flux.stores.entries.AllEntriesStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import jsfacades.Mousetrap
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
    .componentWillMount(scope => scope.backend.configureKeyboardShortcuts(scope.props.router))
    .componentDidMount(scope =>
      LogExceptionsCallback {
        scope.props.router.currentPage match {
          case page: Page.Search => {
            scope.backend.queryInputRef().setValue(page.query)
          }
          case _ =>
        }
    })
    .componentWillReceiveProps(scope => scope.backend.configureKeyboardShortcuts(scope.nextProps.router))
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private type State = Unit
  private class Backend(val $ : BackendScope[Props, State]) {
    val queryInputRef = uielements.input.TextInput.ref()

    def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      def menuItem(label: String, iconClass: String, page: Page): VdomElement =
        router
          .anchorWithHrefTo(page)(
            ^^.ifThen(page.getClass == props.router.currentPage.getClass) { ^.className := "active" },
            <.i(^.className := iconClass),
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
                  placeholder = i18n("facto.search"),
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
                        case None =>
                      }
                    }
                  },
                  <.i(^.className := "fa fa-search")
                )
              )
            ))
        ),
        <.li(
          menuItem(i18n("facto.everything.html"), "icon-list", Page.Everything),
          menuItem(i18n("facto.cash-flow.html"), "icon-money", Page.CashFlow),
          menuItem(i18n("facto.liquidation.html"), "icon-balance-scale", Page.Liquidation),
          menuItem(i18n("facto.endowments.html"), "icon-crown", Page.Endowments),
          menuItem(i18n("facto.summary.html"), "icon-table", Page.Summary)
        ),
        <.li(
          menuItem(i18n("facto.new-entry.html"), "icon-new-empty", Page.NewTransactionGroup()),
          menuItem(i18n("facto.templates.html"), "icon-template", Page.TemplateList)
        )
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
  }

  private case class Props(router: RouterContext)
}
