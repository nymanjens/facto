package hydro.flux.react.uielements

import app.flux.router.AppPages
import hydro.common.I18n
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.uielements.SbadminMenu.MenuItem
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class SbadminMenu(implicit i18n: I18n) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(menuItems: Seq[Seq[MenuItem]], enableSearch: Boolean, router: RouterContext): VdomElement = {
    component(Props(menuItems = menuItems, enableSearch = enableSearch, router = router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(menuItems: Seq[Seq[MenuItem]], enableSearch: Boolean, router: RouterContext)

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with WillMount
      with DidMount
      with WillReceiveProps {
    val queryInputRef = TextInput.ref()

    override def willMount(props: Props, state: State): Callback = configureKeyboardShortcuts(props)

    override def didMount(props: Props, state: State): Callback = LogExceptionsCallback {
      props.router.currentPage match {
        case page: AppPages.Search => {
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
                      ^^.ifThen(page.getClass == props.router.currentPage.getClass) {
                        ^.className := "active"
                      },
                      ^.key := label,
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

      for {
        menuItem <- props.menuItems.flatten
        shortcut <- menuItem.shortcuts
      } {
        bindToPage(shortcut, menuItem.page)
      }

      if (props.enableSearch) {
        bind("shift+alt+f", () => queryInputRef().focus())
      }
    }
  }
}
object SbadminMenu {
  // **************** Public types **************** //
  case class MenuItem(label: String,
                      page: Page,
                      iconClass: Option[String] = None,
                      shortcuts: Seq[String] = Seq())
}
