package app.flux.router

import hydro.common.I18n
import hydro.flux.action.Dispatcher
import hydro.flux.router.Page
import hydro.models.access.EntityAccess
import japgolly.scalajs.react.extra.router._

final class Module(implicit
    reactAppModule: app.flux.react.app.Module,
    dispatcher: Dispatcher,
    i18n: I18n,
    entityAccess: EntityAccess,
) {

  implicit lazy val router: Router[Page] = (new RouterFactory).createRouter()
}
