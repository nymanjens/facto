package common.websocket

import java.nio.ByteBuffer

import common.LoggingUtils.logExceptions
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent, _}

import scala.async.Async.{async, await}
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.{ArrayBuffer, _}

private[websocket] final class SerialWebsocketClient(websocketPath: String) {
  require(!websocketPath.startsWith("/"))

  var openWebsocketPromise: Option[Promise[WebSocket]] = None
  val responseMessagePromises: mutable.Buffer[Promise[ByteBuffer]] = mutable.Buffer()

  def sendAndReceive(request: ByteBuffer): Future[ByteBuffer] = async {
    val lastMessagePromise: Option[Promise[_]] = responseMessagePromises.lastOption
    val thisMessagePromise: Promise[ByteBuffer] = Promise()
    responseMessagePromises += thisMessagePromise

    // Wait until websocket is initialized and previous request is done
    val websocket = await(getOrOpenWebsocket().future)
    if (lastMessagePromise.isDefined) {
      await(lastMessagePromise.get.future)
    }

    logExceptions {
      websocket.send(toArrayBuffer(request))
    }

    await(thisMessagePromise.future)
  }

  def backlogSize: Int = responseMessagePromises.size

  private def getOrOpenWebsocket(): Promise[WebSocket] = {
    if (openWebsocketPromise.isEmpty) {
      openWebsocketPromise = Some(openWebsocket())
    }
    openWebsocketPromise.get
  }

  private def openWebsocket(): Promise[WebSocket] = {
    val protocol = if (dom.window.location.protocol == "https:") "wss:" else "ws:"
    val websocket = new dom.WebSocket(s"${protocol}//${dom.window.location.host}/$websocketPath")
    val websocketPromise: Promise[WebSocket] = Promise()

    websocket.binaryType = "arraybuffer";
    websocket.onmessage = (e: MessageEvent) => {
      val bytes = TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])
      onMessageReceived(bytes)
    }
    websocket.onopen = (e: Event) => {
      websocketPromise.success(websocket)
      // logLine("Opened")
    }

    websocket.onerror = (e: ErrorEvent) => {
      // Note: the given event turns out to be of type "error", but has an undefined message. This causes
      // ClassCastException when accessing it as a String
      val errorMessage = s"Error when connecting to WebSocket"
      websocketPromise.tryFailure(new RuntimeException(errorMessage))
      responseMessagePromises.headOption.map(_.tryFailure(new RuntimeException(errorMessage)))
      logLine(errorMessage)
    }
    websocket.onclose = (e: CloseEvent) => {
      val errorMessage = s"WebSocket was closed: ${e.reason}"
      websocketPromise.tryFailure(new RuntimeException(errorMessage))
      responseMessagePromises.headOption.map(_.tryFailure(new RuntimeException(errorMessage)))
      // logLine(errorMessage)

      this.openWebsocketPromise = None
    }
    websocketPromise
  }

  private def onMessageReceived(bytes: ByteBuffer): Unit = {
    responseMessagePromises.headOption match {
      case Some(promise) if promise.isCompleted =>
        throw new AssertionError("First promise in responseMessagePromises is completed. This is a bug!")
      case Some(promise) =>
        responseMessagePromises.remove(0)
        promise.success(bytes)
      case None =>
        logLine("Warning: Received message without request")
    }
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

  private val websocketNumber: Int = {
    val result = SerialWebsocketClient.nextWebsocketNumber
    SerialWebsocketClient.nextWebsocketNumber += 1
    result
  }
  private def logLine(line: String): Unit = console.log(s"  [WebSocketClient-$websocketNumber] $line")
}
object SerialWebsocketClient {
  private var nextWebsocketNumber: Int = 1
}
