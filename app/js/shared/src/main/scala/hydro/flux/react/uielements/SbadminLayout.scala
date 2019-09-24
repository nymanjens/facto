package hydro.flux.react.uielements

import app.models.user.User
import app.AppVersion
import hydro.common.CollectionUtils.ifThenSeq
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import hydro.models.access.JsEntityAccess
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class SbadminLayout(
    implicit globalMessages: GlobalMessages,
    pageLoadingSpinner: PageLoadingSpinner,
    applicationDisconnectedIcon: ApplicationDisconnectedIcon,
    localDatabaseHasBeenLoadedIcon: LocalDatabaseHasBeenLoadedIcon,
    pendingModificationsCounter: PendingModificationsCounter,
    user: User,
    i18n: I18n,
    jsEntityAccess: JsEntityAccess,
    dispatcher: Dispatcher,
) {

  // **************** API ****************//
  def apply(title: TagMod, leftMenu: VdomElement, pageContent: VdomElement, extraFooter: Seq[TagMod] = Seq())(
      implicit router: RouterContext): VdomElement = {
    <.div(
      ^.id := "wrapper",
      // Navigation
      <.nav(
        ^.className := "navbar navbar-default navbar-static-top",
        ^.style := js.Dictionary("marginBottom" -> 0),
        <.div(
          ^.className := "navbar-header",
          <.button(
            ^.className := "navbar-toggle",
            VdomAttr("data-toggle") := "collapse",
            VdomAttr("data-target") := ".navbar-collapse",
            <.span(^.className := "sr-only", "Toggle navigation"),
            <.span(^.className := "icon-bar"),
            <.span(^.className := "icon-bar"),
            <.span(^.className := "icon-bar")
          ),
          Bootstrap.NavbarBrand(tag = router.anchorWithHrefTo(StandardPages.Root))(title),
          " ",
          pageLoadingSpinner()
        ),
        <.ul(
          ^.className := "nav navbar-top-links navbar-right",
          applicationDisconnectedIcon(),
          pendingModificationsCounter(),
          localDatabaseHasBeenLoadedIcon(),
          <.li(
            ^.className := "dropdown",
            <.a(
              ^.className := "dropdown-toggle",
              VdomAttr("data-toggle") := "dropdown",
              ^.href := "#",
              Bootstrap.FontAwesomeIcon("user", fixedWidth = true),
              " ",
              Bootstrap.FontAwesomeIcon("caret-down"),
            ),
            <.ul(
              ^.className := "dropdown-menu dropdown-user",
              <.li(
                router
                  .anchorWithHrefTo(StandardPages.UserProfile)(
                    <.i(^.className := StandardPages.UserProfile.iconClass),
                    " ",
                    StandardPages.UserProfile.titleSync
                  )),
              ^^.ifThen(user.isAdmin) {
                <.li(
                  router
                    .anchorWithHrefTo(StandardPages.UserAdministration)(
                      <.i(^.className := StandardPages.UserAdministration.iconClass),
                      " ",
                      StandardPages.UserAdministration.titleSync
                    ))
              },
              <.li(^.className := "divider"),
              <.li(
                <.a(
                  ^.href := "/logout/",
                  ^.onClick ==> doLogout,
                  <.i(^.className := "fa fa-sign-out fa-fw"),
                  " ",
                  i18n("app.logout")))
            )
          )
        ),
        <.div(
          ^.className := "navbar-default sidebar",
          ^.role := "navigation",
          <.div(
            ^^.classes(Seq("sidebar-nav", "navbar-collapse") ++ ifThenSeq(navbarCollapsed, "collapse")),
            leftMenu)
        )
      ),
      // Page Content
      <.div(
        ^.id := "page-wrapper",
        ^.style := js.Dictionary("minHeight" -> s"${pageWrapperHeightPx}px"),
        <.div(
          ^.className := "container-fluid",
          Bootstrap.Row(
            Bootstrap.Col(lg = 12)(
              globalMessages(),
              pageContent,
              <.hr(),
              s"v${AppVersion.versionString}",
              <.span(^.style := js.Dictionary("marginLeft" -> "45px")),
              <.span(^.dangerouslySetInnerHtml := "&copy;"),
              " 2019 Jens Nyman"
            )(extraFooter: _*)
          )
        )
      )
    )
  }

  // **************** Private helper methods ****************//
  private def navbarCollapsed: Boolean = {
    // Based on Start Bootstrap code in assets/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
    val width = if (dom.window.innerWidth > 0) dom.window.innerWidth else dom.window.screen.width
    width < 768
  }
  private def pageWrapperHeightPx: Int = {
    // Based on Start Bootstrap code in assets/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
    val topOffset = if (navbarCollapsed) 100 else 50

    val windowHeight = if (dom.window.innerHeight > 0) dom.window.innerHeight else dom.window.screen.height
    windowHeight.toInt - 1 - topOffset
  }

  private def doLogout(e: ReactMouseEvent): Callback = LogExceptionsCallback {
    e.preventDefault()
    dispatcher.dispatch(StandardActions.SetPageLoadingState(isLoading = true))
    jsEntityAccess.clearLocalDatabase() map { _ =>
      dom.window.location.href = "/logout/"
    }
  }
}
