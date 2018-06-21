package common.websocket

import java.nio.ByteBuffer

import common.LoggingUtils.logExceptions
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent, _}

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, _}

final class PushingWebsocketClient(websocketPath: String, onMessageReceived: ByteBuffer => Unit) {
  require(!websocketPath.startsWith("/"))

  openWebsocket()

  private def openWebsocket(): Unit = {
    val protocol = if (dom.window.location.protocol == "https:") "wss:" else "ws:"
    val websocket = new dom.WebSocket(s"${protocol}//${dom.window.location.host}/$websocketPath")

    websocket.binaryType = "arraybuffer"
    websocket.onmessage = (e: MessageEvent) =>
      logExceptions {
        val bytes = TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])
        onMessageReceived(bytes)
    }
    websocket.onopen = (e: Event) =>
      logExceptions {
        logLine("Opened")
    }
    websocket.onerror = (e: ErrorEvent) =>
      logExceptions {
        // Note: the given event turns out to be of type "error", but has an undefined message. This causes
        // ClassCastException when accessing it as a String
        val errorMessage = s"Error when connecting to WebSocket"
        logLine(errorMessage)
    }
    websocket.onclose = (e: CloseEvent) =>
      logExceptions {
        val errorMessage = s"WebSocket was closed: ${e.reason}"
        logLine(errorMessage)
        // TODO: Retry
        //      js.timers.setTimeout(timeout)(logic))
    }
  }

  def close(): Unit = {
    // TODO
  }

  private def logLine(line: String): Unit = console.log(s"  [PushingWebsocketClient] $line")
}
