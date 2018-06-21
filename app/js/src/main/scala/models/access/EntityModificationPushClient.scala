package models.access

import boopickle.Default._
import api.Picklers._
import api.ScalaJsApi.{GetInitialDataResponse, ModificationsWithToken, UpdateToken}
import api.ScalaJsApiClient
import boopickle.Default.Unpickle
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
                                         onMessageReceived: ModificationsWithToken => Future[Unit]) {

  private val firstMessageWasProcessedPromise: Promise[Unit] = Promise()

  private val websocketClient = new PushingWebsocketClient(
    websocketPath = s"websocket/entitymodificationpush/$updateToken/",
    onMessageReceived = bytes =>
      async {
        val modificationsWithToken = Unpickle[ModificationsWithToken].fromBytes(bytes)
        await(onMessageReceived(modificationsWithToken))
        firstMessageWasProcessedPromise.trySuccess((): Unit)
    }
  )

  // TODO: Keep track of latest updateToken and use that to re-open PushingWebsocketClient if it fails

  def firstMessageWasProcessedFuture: Future[Unit] = firstMessageWasProcessedPromise.future

  def close(): Unit = websocketClient.close()
}
