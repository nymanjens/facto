package common.websocket

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import java.nio.ByteBuffer

import common.LoggingUtils.logExceptions
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent, _}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, _}

final class PushingWebsocketClient(websocketPath: String, onMessageReceived: ByteBuffer => Unit) {
  require(!websocketPath.startsWith("/"))

  private val websocketFuture: Future[WebSocket] = openWebsocket()

  private def openWebsocket(): Future[WebSocket] = {
    val protocol = if (dom.window.location.protocol == "https:") "wss:" else "ws:"
    val websocket = new dom.WebSocket(s"${protocol}//${dom.window.location.host}/$websocketPath")
    val websocketPromise: Promise[WebSocket] = Promise()

    websocket.binaryType = "arraybuffer"
    websocket.onmessage = (e: MessageEvent) =>
      logExceptions {
        val bytes = TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])
        onMessageReceived(bytes)
    }
    websocket.onopen = (e: Event) =>
      logExceptions {
        websocketPromise.success(websocket)
        logLine("Opened")
    }
    websocket.onerror = (e: ErrorEvent) =>
      logExceptions {
        // Note: the given event turns out to be of type "error", but has an undefined message. This causes
        // ClassCastException when accessing it as a String
        val errorMessage = s"Error when connecting to WebSocket"
        websocketPromise.tryFailure(new RuntimeException(errorMessage))
        logLine(errorMessage)
    }
    websocket.onclose = (e: CloseEvent) =>
      logExceptions {
        val errorMessage = s"WebSocket was closed: ${e.reason}"
        websocketPromise.tryFailure(new RuntimeException(errorMessage))
        logLine(errorMessage)
    }

    websocketPromise.future
  }

  def close(): Unit = async {
    val websocket = await(websocketFuture)
    websocket.onclose = (e: CloseEvent) => {}
    websocket.close()
  }

  private def logLine(line: String): Unit = console.log(s"  [PushingWebsocketClient] $line")
}
