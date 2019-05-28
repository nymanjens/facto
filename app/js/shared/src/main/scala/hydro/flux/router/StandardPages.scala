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
  case class Search private (encodedQuery: String) extends Page {
    def query: String = {
      val decoded = js.URIUtils.decodeURIComponent(js.URIUtils.decodeURI(encodedQuery))
      decoded
        .replace('+', ' ') // Hack: The Chrome 'search engine' feature translates space into plus
        .replace(Search.escapedPlus, "+") // Unescape plus in case it was entered in a search <input>
    }

    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.search-results-for", query))
    override def iconClass = "icon-list"
  }
  object Search {

    private val escapedPlus = "_PLUS_"

    def fromInput(query: String): Search = {
      val manuallyEscapedQuery = query.replace("+", escapedPlus)
      new Search(js.URIUtils.encodeURIComponent(manuallyEscapedQuery))
    }
  }
}
