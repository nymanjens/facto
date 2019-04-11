package app.flux.react.app

import hydro.common.I18n
import app.flux.router.AppPages
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.accounting.config.Template
import app.models.user.User
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[app] final class TemplateList(implicit user: User,
                                      accountingConfig: Config,
                                      entityAccess: AppJsEntityAccess,
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
                  Bootstrap.Button(
                    Variant.info,
                    Size.lg,
                    tag = router.anchorWithHrefTo(AppPages.NewFromTemplate(template)))(
                    ^.key := template.code,
                    <.i(^.className := template.iconClass),
                    " ",
                    template.name)
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
