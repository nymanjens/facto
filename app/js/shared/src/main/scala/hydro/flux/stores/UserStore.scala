package hydro.flux.stores

import app.api.ScalaJsApiClient
import hydro.models.modification.EntityModification
import app.models.user.User
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions.UpsertUser
import hydro.flux.stores.UserStore.State
import hydro.models.access.JsEntityAccess

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class UserStore(
    implicit dispatcher: Dispatcher,
    scalaJsApiClient: ScalaJsApiClient,
    entityAccess: JsEntityAccess,
) extends AsyncEntityDerivedStateStore[State] {

  dispatcher.registerPartialAsync {
    case UpsertUser(userPrototype) =>
      scalaJsApiClient.upsertUser(userPrototype)
  }

  override protected def calculateState(): Future[State] = async {
    val allUsers = await(entityAccess.newQuery[User]().data())
    State(allUsers = allUsers)
  }

  override protected def modificationImpactsState(
      entityModification: EntityModification,
      state: State,
  ): Boolean =
    entityModification.entityType == User.Type
}

object UserStore {
  case class State(allUsers: Seq[User])
}
