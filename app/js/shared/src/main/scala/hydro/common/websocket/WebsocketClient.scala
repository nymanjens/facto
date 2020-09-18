package hydro.common.websocket

import java.nio.ByteBuffer

import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.websocket.WebsocketClient.TypeSpecificConversion
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.CloseEvent
import org.scalajs.dom.Event
import org.scalajs.dom.MessageEvent
import org.scalajs.dom.WebSocket

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js.typedarray._
import scala.scalajs.js.typedarray.ArrayBuffer

final class WebsocketClient[T](name: String, jsWebsocket: WebSocket)(implicit
    typeSpecificConversion: TypeSpecificConversion[T]
) {

  def send(message: T): Unit = logExceptions {
    typeSpecificConversion.send(jsWebsocket, message)
  }

  def close(): Unit = {
    jsWebsocket.onclose = (e: CloseEvent) => {}
    jsWebsocket.close()
    WebsocketClient.logLine(name, "Closed WebSocket")
  }
}
object WebsocketClient {
  def open[T](
      name: String,
      websocketPath: String,
      onOpen: () => Unit = () => {},
      onMessageReceived: T => Unit,
      onError: () => Unit = () => {},
      onClose: () => Unit = () => {},
  )(implicit typeSpecificConversion: TypeSpecificConversion[T]): Future[WebsocketClient[T]] = {
    require(!websocketPath.startsWith("/"))

    logLine(name, "Opening...")
    val protocol = if (dom.window.location.protocol == "https:") "wss:" else "ws:"
    val jsWebsocket = new dom.WebSocket(s"${protocol}//${dom.window.location.host}/$websocketPath")
    val resultPromise: Promise[WebsocketClient[T]] = Promise()

    jsWebsocket.binaryType = "arraybuffer"
    jsWebsocket.onmessage = (e: MessageEvent) =>
      logExceptions {
        onMessageReceived(typeSpecificConversion.extractMessage(e))
      }
    jsWebsocket.onopen = (e: Event) =>
      logExceptions {
        resultPromise.success(new WebsocketClient(name, jsWebsocket))
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

  trait TypeSpecificConversion[T] {
    def send(jsWebsocket: WebSocket, message: T): Unit
    def extractMessage(jsMessageEvent: MessageEvent): T
  }
  object TypeSpecificConversion {

    implicit object Binary extends TypeSpecificConversion[ByteBuffer] {

      override def send(jsWebsocket: WebSocket, message: ByteBuffer): Unit = {
        jsWebsocket.send(toArrayBuffer(message))
      }

      override def extractMessage(jsMessageEvent: MessageEvent): ByteBuffer =
        TypedArrayBuffer.wrap(jsMessageEvent.data.asInstanceOf[ArrayBuffer])

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

    implicit object ForStrings extends TypeSpecificConversion[String] {
      override def send(jsWebsocket: WebSocket, message: String): Unit = {
        jsWebsocket.send(message)
      }

      override def extractMessage(jsMessageEvent: MessageEvent): String = {
        jsMessageEvent.data.asInstanceOf[String]
      }
    }
  }
}
