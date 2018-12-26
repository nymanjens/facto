package app.flux.router

import common.I18n
import hydro.flux.action.Dispatcher
import japgolly.scalajs.react.extra.router._
import app.models.access.EntityAccess

final class Module(implicit
                   reactAppModule: app.flux.react.app.Module,
                   dispatcher: Dispatcher,
                   i18n: I18n,
                   entityAccess: EntityAccess,
) {

  implicit lazy val router: Router[Page] = (new RouterFactory).createRouter()
}
