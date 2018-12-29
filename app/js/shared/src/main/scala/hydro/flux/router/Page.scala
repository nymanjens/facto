package hydro.flux.router

import app.models.access.EntityAccess
import common.I18n

import scala.concurrent.Future

trait Page {
  def title(implicit i18n: I18n, entityAccess: EntityAccess): Future[String]
  def iconClass: String
}
