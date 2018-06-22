package models.access

import api.Picklers._
import api.ScalaJsApi.{ModificationsWithToken, UpdateToken}
import boopickle.Default.{Unpickle, _}
import common.websocket.BinaryWebsocketClient

import scala.async.Async.{async, await}
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class EntityModificationPushClient(updateToken: UpdateToken,
                                         onMessageReceived: ModificationsWithToken => Future[Unit]) {

  private val firstMessageWasProcessedPromise: Promise[Unit] = Promise()

  private val websocketClient = BinaryWebsocketClient.open(
    name = "EntityModificationPushClient",
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

  def close(): Unit = websocketClient.map(_.close())
}
