package hydro.flux.router

import hydro.common.I18n
import hydro.flux.router.Page.PageBase
import hydro.models.access.EntityAccess

import scala.concurrent.Future
import scala.scalajs.js

object StandardPages {
  case object Root extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) = Future.successful("Root")
    override def iconClass = ""
  }

  // **************** User management views **************** //
  case object UserProfile extends PageBase("app.user-profile", iconClass = "fa fa-user fa-fw")
  case object UserAdministration extends PageBase("app.user-administration", iconClass = "fa fa-cogs fa-fw")

  // **************** Menu bar search **************** //
  case class Search(encodedQuery: String) extends Page {
    def query: String = js.URIUtils.decodeURIComponent(js.URIUtils.decodeURI(encodedQuery))

    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.search-results-for", query))
    override def iconClass = "icon-list"
  }
  object Search {
    def apply(query: String): Search = new Search(js.URIUtils.encodeURIComponent(query))
  }
}
