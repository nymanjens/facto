package app.flux.react.app

import common.I18n
import common.LoggingUtils.logExceptions
import app.flux.react.ReactVdomUtils.<<
import app.flux.router.Page
import app.flux.router.RouterContext
import app.flux.react.uielements
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import app.models.access.EntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.user.User

import scala.collection.immutable.Seq

private[app] final class TemplateList(implicit user: User,
                                      accountingConfig: Config,
                                      entityAccess: EntityAccess,
                                      i18n: I18n,
                                      pageHeader: PageHeader,
) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) =>
      logExceptions {
        implicit val router = props.router
        <.span(
          pageHeader(router.currentPage),
          Panel(
            title = i18n("app.templates"),
            panelClasses = Seq("templates-panel")
          ) {
            val templates = accountingConfig.templatesToShowFor(Template.Placement.TemplateList, user)
            <<.joinWithSpaces(
              for (template <- templates)
                yield
                  router.anchorWithHrefTo(Page.NewFromTemplate(template))(
                    ^.key := template.code,
                    ^.className := "btn btn-info btn-lg",
                    <.i(^.className := template.iconClass),
                    " ",
                    template.name
                  )
            )
          }
        )
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
