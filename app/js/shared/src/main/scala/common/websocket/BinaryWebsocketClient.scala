package common.websocket

import java.nio.ByteBuffer

import common.LoggingUtils.logExceptions
import org.scalajs.dom
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.ErrorEvent
import org.scalajs.dom.Event
import org.scalajs.dom.MessageEvent
import org.scalajs.dom._

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.scalajs.js.typedarray._

final class BinaryWebsocketClient(name: String, jsWebsocket: WebSocket) {

  def send(message: ByteBuffer): Unit = logExceptions {
    jsWebsocket.send(toArrayBuffer(message))
  }

  def close(): Unit = {
    jsWebsocket.onclose = (e: CloseEvent) => {}
    jsWebsocket.close()
    BinaryWebsocketClient.logLine(name, "Closed WebSocket")
  }

  private def toArrayBuffer(byteBuffer: ByteBuffer): ArrayBuffer = {
    val length = byteBuffer.remaining()
    val arrayBuffer = new ArrayBuffer(length)
    var arrayBufferView = new Int8Array(arrayBuffer)
    for (i <- 0 until length) {
      arrayBufferView.set(i, byteBuffer.get())
    }
    arrayBuffer
  }
}
object BinaryWebsocketClient {
  def open(name: String,
           websocketPath: String,
           onOpen: () => Unit = () => {},
           onMessageReceived: ByteBuffer => Unit,
           onError: () => Unit = () => {},
           onClose: () => Unit = () => {}): Future[BinaryWebsocketClient] = {
    require(!websocketPath.startsWith("/"))

    logLine(name, "Opening...")
    val protocol = if (dom.window.location.protocol == "https:") "wss:" else "ws:"
    val jsWebsocket = new dom.WebSocket(s"${protocol}//${dom.window.location.host}/$websocketPath")
    val resultPromise: Promise[BinaryWebsocketClient] = Promise()

    jsWebsocket.binaryType = "arraybuffer"
    jsWebsocket.onmessage = (e: MessageEvent) =>
      logExceptions {
        val bytes = TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])
        onMessageReceived(bytes)
    }
    jsWebsocket.onopen = (e: Event) =>
      logExceptions {
        resultPromise.success(new BinaryWebsocketClient(name, jsWebsocket))
        logLine(name, "Opened")
        onOpen()
    }
    jsWebsocket.onerror = (e: Event) =>
      logExceptions {
        // Note: the given event turns out to be of type "error", but has an undefined message. This causes
        // ClassCastException when accessing it as a String
        val errorMessage = s"Error from WebSocket"
        resultPromise.tryFailure(new RuntimeException(errorMessage))
        logLine(name, errorMessage)
        onError()
    }
    jsWebsocket.onclose = (e: CloseEvent) =>
      logExceptions {
        val errorMessage = s"WebSocket was closed"
        resultPromise.tryFailure(new RuntimeException(errorMessage))
        logLine(name, errorMessage)
        onClose()
    }

    resultPromise.future
  }

  private def logLine(name: String, line: String): Unit = console.log(s"  [$name] $line")
}
