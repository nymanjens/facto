package models.access

import api.Picklers._
import api.ScalaJsApi.{ModificationsWithToken, UpdateToken}
import boopickle.Default.{Unpickle, _}
import common.websocket.BinaryWebsocketClient
import org.scalajs.dom
import org.scalajs.dom.console
import org.scalajs.dom.raw.Event

import scala.async.Async.{async, await}
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class EntityModificationPushClient(name: String,
                                         updateToken: UpdateToken,
                                         onMessageReceived: ModificationsWithToken => Future[Unit]) {

  private val firstMessageWasProcessedPromise: Promise[Unit] = Promise()

  private var lastUpdateToken: UpdateToken = updateToken

  private var websocketClient: Option[Future[BinaryWebsocketClient]] = Some(openWebsocketClient(updateToken))

  private val onlineListener: js.Function1[Event, Unit] = _ => {
    if (websocketClient.isEmpty) {
      websocketClient = Some(openWebsocketClient(lastUpdateToken))
    }
  }

  dom.window.addEventListener("online", onlineListener)

  def firstMessageWasProcessedFuture: Future[Unit] = firstMessageWasProcessedPromise.future

  def close(): Unit = {
    if (websocketClient.isDefined) {
      websocketClient.get.map(_.close())
    }
    websocketClient = Some(Future.failed(new IllegalStateException("WebSocket is closed")))
    dom.window.removeEventListener("online", onlineListener)
  }

  private def openWebsocketClient(updateToken: UpdateToken): Future[BinaryWebsocketClient] = {
    BinaryWebsocketClient.open(
      name = name,
      websocketPath = s"websocket/entitymodificationpush/$updateToken/",
      onMessageReceived = bytes =>
        async {
          val modificationsWithToken = Unpickle[ModificationsWithToken].fromBytes(bytes)
          await(onMessageReceived(modificationsWithToken))
          firstMessageWasProcessedPromise.trySuccess((): Unit)
      },
      onClose = () => {
        websocketClient = None
        js.timers.setTimeout(10.seconds) {
          if (websocketClient.isEmpty) {
            websocketClient = Some(openWebsocketClient(lastUpdateToken))
          }
        }
      }
    )
  }
}
