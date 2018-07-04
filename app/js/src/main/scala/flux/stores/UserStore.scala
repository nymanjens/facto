package flux.stores

import api.ScalaJsApiClient
import flux.action.Action.{RemoveBalanceCheck, UpdateBalanceCheck, UpsertUser}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import flux.action.Dispatcher
import flux.stores.UserStore.State
import models.access.JsEntityAccess
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class UserStore(implicit dispatcher: Dispatcher,
                      scalaJsApiClient: ScalaJsApiClient,
                      entityAccess: JsEntityAccess)
    extends AsyncEntityDerivedStateStore[State] {

  dispatcher.registerPartialAsync {
    case UpsertUser(userPrototype) =>
      scalaJsApiClient.upsertUser(userPrototype)
  }

  override protected def calculateState(): Future[State] = async {
    val allUsers = await(entityAccess.newQuery[User]().data())
    State(allUsers = allUsers)
  }

  override protected def modificationImpactsState(entityModification: EntityModification,
                                                  state: State): Boolean =
    entityModification.entityType == EntityType.UserType
}

object UserStore {
  case class State(allUsers: Seq[User])
}
