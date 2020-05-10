package hydro.models.access

import java.nio.ByteBuffer
import java.time
import java.time.Instant

import app.api.ScalaJsApi.HydroPushSocketPacket
import app.api.ScalaJsApi.HydroPushSocketPacket.EntityModificationsWithToken
import app.api.ScalaJsApi.UpdateToken
import boopickle.Default.Unpickle
import boopickle.Default._
import app.api.Picklers._
import app.AppVersion
import hydro.common.Listenable
import hydro.common.Listenable.WritableListenable
import hydro.common.time.Clock
import hydro.common.websocket.WebsocketClient
import org.scalajs.dom
import org.scalajs.dom.raw.Event
import hydro.common.time.JavaTimeImplicits._
import org.scalajs

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class HydroPushSocketClientFactory(implicit clock: Clock) {

  private val _pushClientsAreOnline: WritableListenable[Boolean] = WritableListenable(true)

  private[access] def createClient(
      name: String,
      updateToken: UpdateToken,
      onMessageReceived: EntityModificationsWithToken => Future[Unit],
  ): HydroPushSocketClient =
    new HydroPushSocketClient(name, updateToken, onMessageReceived)

  /** Returns true if a push client socket is open or if there is no reason to believe it wouldn't be able to open. */
  def pushClientsAreOnline: Listenable[Boolean] = _pushClientsAreOnline

  private[access] final class HydroPushSocketClient private[HydroPushSocketClientFactory] (
      name: String,
      updateToken: UpdateToken,
      onMessageReceived: EntityModificationsWithToken => Future[Unit],
  ) {

    private val firstMessageWasProcessedPromise: Promise[Unit] = Promise()
    private var lastUpdateToken: UpdateToken = updateToken
    private var lastStartToOpenTime: Instant = clock.nowInstant
    private var lastPacketTime: Instant = clock.nowInstant

    private var isClosed = false
    private var websocketClient: Option[Future[WebsocketClient[ByteBuffer]]] = Some(
      openWebsocketClient(updateToken))

    private val onlineListener: js.Function1[Event, Unit] = _ => openWebsocketIfEmpty()

    dom.window.addEventListener("online", onlineListener)
    dom.window.addEventListener("focus", onlineListener)
    startCheckingLastPacketTimeNotTooLongAgo()

    def firstMessageWasProcessedFuture: Future[Unit] = firstMessageWasProcessedPromise.future

    def close(): Unit = {
      if (websocketClient.isDefined) {
        websocketClient.get.map(_.close())
      }
      websocketClient = Some(Future.failed(new IllegalStateException("WebSocket is closed")))
      isClosed = true
      dom.window.removeEventListener("online", onlineListener)
      dom.window.removeEventListener("focus", onlineListener)
    }

    private def openWebsocketIfEmpty() = {
      if (websocketClient.isEmpty) {
        websocketClient = Some(openWebsocketClient(lastUpdateToken))
      }
    }

    private def openWebsocketClient(updateToken: UpdateToken): Future[WebsocketClient[ByteBuffer]] = {
      lastStartToOpenTime = clock.nowInstant
      WebsocketClient.open[ByteBuffer](
        name = name,
        websocketPath = s"websocket/hydropush/$updateToken/",
        onMessageReceived = bytes =>
          async {
            val packet = Unpickle[HydroPushSocketPacket].fromBytes(bytes)
            packet match {
              case modificationsWithToken: EntityModificationsWithToken =>
                await(onMessageReceived(modificationsWithToken))
                firstMessageWasProcessedPromise.trySuccess((): Unit)
              case HydroPushSocketPacket.Heartbeat => // Do nothing
              case HydroPushSocketPacket.VersionCheck(versionString) =>
                if (versionString != AppVersion.versionString) {
                  println("  Detected that client version is outdated. Will reload page...")
                  dom.window.location.reload( /* forcedReload = */ true)
                }
            }
            _pushClientsAreOnline.set(true)
            lastPacketTime = clock.nowInstant
        },
        onClose = () => {
          websocketClient = None
          js.timers.setTimeout(10.seconds)(openWebsocketIfEmpty())
          _pushClientsAreOnline.set(false)
          firstMessageWasProcessedPromise.tryFailure(
            new RuntimeException(s"[$name] WebSocket was closed before first message was processed"))
        }
      )
    }

    /**
      * This method checks that we get regular heartbeats. If not, it closes and re-opens the connection.
      *
      * This aims to solve a bug that sometimes the connection seems to be open while nothing actually gets received.
      */
    private def startCheckingLastPacketTimeNotTooLongAgo(): Unit = {
      val timeoutDuration = 15.seconds
      def cyclicLogic(): Unit = {
        websocketClient match {
          case Some(clientFuture)
              if clientFuture.isCompleted &&
                !isClosed &&
                (clock.nowInstant - lastPacketTime) > java.time.Duration.ofSeconds(10) &&
                (clock.nowInstant - lastStartToOpenTime) > java.time.Duration.ofSeconds(10) =>
            println(
              s"  [$name] WebSocket didn't receive heartbeat for $timeoutDuration. Closing and restarting connection")
            websocketClient = None
            clientFuture.value.get.get.close()

            openWebsocketIfEmpty()
          case _ =>
        }

        if (!isClosed) {
          js.timers.setTimeout(timeoutDuration)(cyclicLogic())
        }
      }
      cyclicLogic()
    }
  }
}
