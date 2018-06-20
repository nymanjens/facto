package models.access

import api.ScalaJsApi.{GetEntityModificationsResponse, GetInitialDataResponse, UpdateToken}
import api.ScalaJsApiClient
import common.LoggingUtils.logFailure
import common.ScalaUtils.visibleForTesting
import common.websocket.PullingWebsocketClient
import models.Entity
import models.access.SingletonKey.NextUpdateTokenKey
import models.modification.{EntityModification, EntityType}
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class EntityModificationPuller(updateToken: UpdateToken,
                                     onMessageReceived: GetEntityModificationsResponse => Unit) {

  private val websocketClient = new PullingWebsocketClient(
    websocketPath = s"websocket/entitymodificationpush/$updateToken/",
    onMessageReceived = bytes => ???
  )

  // TODO: Keep track of latest updateToken and use that to re-open PullingWebsocketClient if it fails

  def firstMessageWasProcessedFuture: Future[Unit] = ???

  def close(): Future[Unit] = ???
}
