package hydro.flux.router

import app.flux.router.RouterFactory
import app.models.access.EntityAccess
import common.I18n
import japgolly.scalajs.react.extra.router.Path

import scala.concurrent.Future
import scala.scalajs.js

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
