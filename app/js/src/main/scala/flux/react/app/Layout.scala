package flux.react.app

import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.html_<^._
import models.User

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
          ^.style := js.Dictionary("margin-bottom" -> 0),
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
                    ^.href := "@routes.Application.profile",
                    <.i(^.className := "fa fa-user fa-fw"),
                    " ",
                    i18n("facto.user-profile"))),
                ^^.ifThen(user.loginName == "admin") {
                  <.li(
                    <.a(
                      ^.href := "@routes.Application.administration",
                      <.i(^.className := "fa fa-cogs fa-fw"),
                      " ",
                      i18n("facto.user-administration")))
                },
                <.li(^.className := "divider"),
                <.li(
                  <.a(
                    ^.href := "@routes.Auth.logout",
                    " ",
                    <.i(^.className := "fa fa-sign-out fa-fw"),
                    i18n("facto.logout")))
              )
            )
          ),
          <.div(
            ^.className := "navbar-default sidebar",
            ^.role := "navigation",
            <.div(^.className := "sidebar-nav navbar-collapse", menu(router))
          )
        ),
        // Page Content
        <.div(
          ^.id := "page-wrapper",
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

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
