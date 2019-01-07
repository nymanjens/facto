package hydro.flux.router

import hydro.common.I18n
import hydro.models.access.EntityAccess

import scala.concurrent.Future

trait Page {
  def title(implicit i18n: I18n, entityAccess: EntityAccess): Future[String]
  def iconClass: String
}
object Page {
  abstract class PageBase(titleKey: String, override val iconClass: String) extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) = Future.successful(titleSync)
    def titleSync(implicit i18n: I18n) = i18n(titleKey)
  }
}
