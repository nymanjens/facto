package hydro.flux.router

import app.models.access.EntityAccess
import common.I18n
import hydro.flux.router.Page.PageBase

import scala.concurrent.Future

object StandardPages {
  case object Root extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) = Future.successful("Root")
    override def iconClass = ""
  }

  // **************** User management views **************** //
  case object UserProfile extends PageBase("app.user-profile", iconClass = "fa fa-user fa-fw")
  case object UserAdministration extends PageBase("app.user-administration", iconClass = "fa fa-cogs fa-fw")
}
