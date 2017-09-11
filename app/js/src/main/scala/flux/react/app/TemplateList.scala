package flux.react.app

import common.I18n
import common.LoggingUtils.logExceptions
import flux.react.router.{Page, RouterContext}
import flux.react.uielements.Panel
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.{EntityAccess, User}
import models.accounting.config.{Config, Template}

import scala.collection.immutable.Seq

private[app] final class TemplateList(implicit user: User,
                                      accountingConfig: Config,
                                      entityAccess: EntityAccess,
                                      i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) =>
      logExceptions {
        implicit val router = props.router
        Panel(
          title = i18n("facto.templates"),
          panelClasses = Seq("templates-panel")
        ) {
          val templates = accountingConfig.templatesToShowFor(Template.Placement.EverythingView, user)

          (
            for (template <- templates)
              yield
                router.anchorWithHrefTo(Page.NewFromTemplate(template))(
                  ^.key := template.code,
                  ^.className := "btn btn-info btn-lg",
                  <.i(^.className := template.iconClass),
                  template.name
                )
          ).toVdomArray
        }
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
