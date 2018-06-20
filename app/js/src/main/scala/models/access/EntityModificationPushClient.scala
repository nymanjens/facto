package models.access

import api.ScalaJsApi.{ModificationsWithToken, GetInitialDataResponse, UpdateToken}
import api.ScalaJsApiClient
import common.LoggingUtils.logFailure
import common.ScalaUtils.visibleForTesting
import common.websocket.PushingWebsocketClient
import models.Entity
import models.access.SingletonKey.NextUpdateTokenKey
import models.modification.{EntityModification, EntityType}
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class EntityModificationPushClient(updateToken: UpdateToken,
                                         onMessageReceived: ModificationsWithToken => Unit) {

  private val websocketClient = new PushingWebsocketClient(
    websocketPath = s"websocket/entitymodificationpush/$updateToken/",
    onMessageReceived = bytes => ???
  )

  // TODO: Keep track of latest updateToken and use that to re-open PushingWebsocketClient if it fails

  def firstMessageWasProcessedFuture: Future[Unit] = ???

  def close(): Future[Unit] = ???
}
