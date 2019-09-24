package hydro.flux.react.uielements

import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.SbadminMenu.MenuItem
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class SbadminMenu(implicit i18n: I18n) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      menuItems: Seq[Seq[MenuItem]],
      enableSearch: Boolean,
      router: RouterContext,
      configureAdditionalKeyboardShortcuts: () => Unit = () => {},
  ): VdomElement = {
    component(
      Props(
        menuItems = menuItems,
        enableSearch = enableSearch,
        router = router,
        configureAdditionalKeyboardShortcuts = configureAdditionalKeyboardShortcuts))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      menuItems: Seq[Seq[MenuItem]],
      enableSearch: Boolean,
      router: RouterContext,
      configureAdditionalKeyboardShortcuts: () => Unit,
  )

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with WillMount
      with DidMount
      with WillReceiveProps {
    val queryInputRef = TextInput.ref()

    override def willMount(props: Props, state: State): Callback = configureKeyboardShortcuts(props)

    override def didMount(props: Props, state: State): Callback = LogExceptionsCallback {
      props.router.currentPage match {
        case page: StandardPages.Search => {
          queryInputRef().setValue(page.query)
        }
        case _ =>
      }
    }

    override def willReceiveProps(currentProps: Props, nextProps: Props, state: State): Callback =
      configureKeyboardShortcuts(nextProps)

    override def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router

      <.ul(
        ^.className := "nav",
        ^.id := "side-menu",
        <<.ifThen(props.enableSearch) {
          <.li(
            ^.className := "sidebar-search",
            <.form(
              Bootstrap.InputGroup(
                ^.className := "custom-search-form",
                TextInput(
                  ref = queryInputRef,
                  name = "query",
                  placeholder = i18n("app.search"),
                  classes = Seq("form-control")),
                Bootstrap.InputGroupButton(
                  Bootstrap.Button(variant = Variant.default, tpe = "submit")(
                    ^.onClick ==> { (e: ReactEventFromInput) =>
                      LogExceptionsCallback {
                        e.preventDefault()

                        queryInputRef().value match {
                          case Some(query) => props.router.setPage(StandardPages.Search.fromInput(query))
                          case None        =>
                        }
                      }
                    },
                    Bootstrap.FontAwesomeIcon("search"),
                  )
                )
              ))
          )
        },
        (
          for {
            (menuItemLi, menuItemLiIndex) <- props.menuItems.zipWithIndex
            if menuItemLi.nonEmpty
          } yield {
            <.li(
              ^.key := menuItemLiIndex,
              (
                for (MenuItem(label, page, iconClass, shortcuts) <- menuItemLi) yield {
                  router
                    .anchorWithHrefTo(page)(
                      ^^.ifThen(page == props.router.currentPage) { ^.className := "active" },
                      // Add underscore to force rerender to fix bug when mouse is on current menu item
                      ^.key := (page.toString + (if (page == props.router.currentPage) "_" else "")),
                      <.i(^.className := iconClass getOrElse page.iconClass),
                      " ",
                      <.span(^.dangerouslySetInnerHtml := label)
                    )
                }
              ).toVdomArray
            )
          }
        ).toVdomArray,
      )
    }

    def configureKeyboardShortcuts(props: Props): Callback = LogExceptionsCallback {
      implicit val router = props.router

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
      def goToAdjacentMenuItem(step: Int): Unit = {
        val allLeftMenuPages = props.menuItems.flatten.map(_.page)
        allLeftMenuPages.indexOf(router.currentPage) match {
          case -1 =>
          case i if 0 <= i + step && i + step < allLeftMenuPages.size =>
            router.setPage(allLeftMenuPages(i + step))
          case _ =>
        }
      }

      bind("shift+alt+up", () => goToAdjacentMenuItem(step = -1))
      bind("shift+alt+down", () => goToAdjacentMenuItem(step = +1))

      for {
        menuItem <- props.menuItems.flatten
        shortcut <- menuItem.shortcuts
      } {
        bindToPage(shortcut, menuItem.page)
      }

      if (props.enableSearch) {
        bind("shift+alt+f", () => queryInputRef().focus())
      }

      props.configureAdditionalKeyboardShortcuts()
    }
  }
}
object SbadminMenu {
  // **************** Public types **************** //
  case class MenuItem(
      label: String,
      page: Page,
      iconClass: Option[String] = None,
      shortcuts: Seq[String] = Seq(),
  )
}
