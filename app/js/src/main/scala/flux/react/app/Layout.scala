package flux.react.app

import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.html_<^._
import models.User
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.scalajs.js

final class Layout(implicit globalMessages: GlobalMessages, menu: Menu, user: User, i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      val router = props.router
      <.div(
        ^.id := "wrapper",
        // Navigation
        <.nav(
          ^.className := "navbar navbar-default navbar-static-top",
          ^.role := "navigation",
          ^.style := js.Dictionary("marginBottom" -> 0),
          <.div(
            ^.className := "navbar-header",
            <.button(
              ^.tpe := "button",
              ^.className := "navbar-toggle",
              VdomAttr("data-toggle") := "collapse",
              VdomAttr("data-target") := ".navbar-collapse",
              <.span(^.className := "sr-only", "Toggle navigation"),
              <.span(^.className := "icon-bar"),
              <.span(^.className := "icon-bar"),
              <.span(^.className := "icon-bar")
            ),
            router.anchorWithHrefTo(Page.Root)(^.className := "navbar-brand", "Family Accounting Tool")
          ),
          <.ul(
            ^.className := "nav navbar-top-links navbar-right",
            <.li(
              ^.className := "dropdown",
              <.a(
                ^.className := "dropdown-toggle",
                VdomAttr("data-toggle") := "dropdown",
                ^.href := "#",
                <.i(^.className := "fa fa-user fa-fw"),
                " ",
                <.i(^.className := "fa fa-caret-down")
              ),
              <.ul(
                ^.className := "dropdown-menu dropdown-user",
                <.li(
                  <.a(
                    ^.href := "/profile/",
                    <.i(^.className := "fa fa-user fa-fw"),
                    " ",
                    i18n("facto.user-profile"))),
                ^^.ifThen(user.loginName == "admin") {
                  <.li(
                    <.a(
                      ^.href := "/administration/",
                      <.i(^.className := "fa fa-cogs fa-fw"),
                      " ",
                      i18n("facto.user-administration")))
                },
                <.li(^.className := "divider"),
                <.li(
                  <.a(
                    ^.href := "/logout/",
                    <.i(^.className := "fa fa-sign-out fa-fw"),
                    " ",
                    i18n("facto.logout")))
              )
            )
          ),
          <.div(
            ^.className := "navbar-default sidebar",
            ^.role := "navigation",
            <.div(
              ^^.classes(Seq("sidebar-nav", "navbar-collapse") ++ ifThenSeq(navbarCollapsed, "collapse")),
              menu(router))
          )
        ),
        // Page Content
        <.div(
          ^.id := "page-wrapper",
          ^.style := js.Dictionary("minHeight" -> s"${pageWrapperHeight}px"),
          <.div(
            ^.className := "container-fluid",
            <.div(
              ^.className := "row",
              <.div(
                ^.className := "col-lg-12",
                globalMessages(),
                children,
                <.hr(),
                <.span(^.dangerouslySetInnerHtml := "&copy;"),
                " 2016 Jens Nyman"))
          )
        )
      )
    }
    .build

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router))(children: _*)
  }

  // **************** Private helper methods ****************//
  def navbarCollapsed: Boolean = {
    // Based on Start Bootstrap code in bower_components/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
    val width = if (dom.window.innerWidth > 0) dom.window.innerWidth else dom.window.screen.width
    width < 768
  }
  def pageWrapperHeight: Int = {
    // Based on Start Bootstrap code in bower_components/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
    val topOffset = if (navbarCollapsed) 100 else 50

    val windowHeight = if (dom.window.innerHeight > 0) dom.window.innerHeight else dom.window.screen.height
    windowHeight.toInt - 1 - topOffset
  }

  private def ifThenSeq[V](condition: Boolean, value: V): Seq[V] = if (condition) Seq(value) else Seq()

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
