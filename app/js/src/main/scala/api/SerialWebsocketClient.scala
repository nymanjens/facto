package api

import java.nio.ByteBuffer

import api.Picklers._
import common.LoggingUtils.logExceptions
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent, _}

import scala.async.Async.{async, await}
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.{ArrayBuffer, _}

private[api] final class SerialWebsocketClient(websocketPath: String) {
  require(!websocketPath.startsWith("/"))

  var (websocket, websocketReadyPromise): (WebSocket, Promise[Unit]) = initWebsocket()
  val responseMessagePromises: mutable.Buffer[Promise[ByteBuffer]] = mutable.Buffer()

  def sendAndReceive(request: ByteBuffer): Future[ByteBuffer] = async {
    val lastMessagePromise: Option[Promise[_]] = responseMessagePromises.lastOption
    val thisMessagePromise: Promise[ByteBuffer] = Promise()
    responseMessagePromises += thisMessagePromise

    // Wait until websocket is initialized and previous request is done
    await(websocketReadyPromise.future)
    if (lastMessagePromise.isDefined) {
      await(lastMessagePromise.get.future)
    }

    logExceptions {
      websocket.send(toArrayBuffer(request))
    }

    await(thisMessagePromise.future)
  }

  private def initWebsocket(): (WebSocket, Promise[Unit]) = {
    val websocket = new dom.WebSocket(s"ws://${dom.window.location.host}/$websocketPath")
    val websocketReadyPromise: Promise[Unit] = Promise()

    websocket.binaryType = "arraybuffer";
    websocket.onmessage = (e: MessageEvent) => {
      val bytes = TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])
      onMessageReceived(bytes)
    }
    websocket.onopen = (e: Event) => {
      websocketReadyPromise.success((): Unit)
      logLine("Opened")
    }

    websocket.onerror = (e: ErrorEvent) => {
      val errorMessage = s"Error has occured: ${e.message}"
      websocketReadyPromise.tryFailure(new RuntimeException(errorMessage))
      responseMessagePromises.headOption.map(_.tryFailure(new RuntimeException(errorMessage)))
      logLine(errorMessage)
    }
    websocket.onclose = (e: CloseEvent) => {
      val errorMessage = s"WebSocket was closed: ${e.reason}"
      websocketReadyPromise.tryFailure(new RuntimeException(errorMessage))
      responseMessagePromises.headOption.map(_.tryFailure(new RuntimeException(errorMessage)))
      logLine(errorMessage)

      val (newWebsocket, newWebsocketReadyPromise) = initWebsocket()
      this.websocket = newWebsocket
      this.websocketReadyPromise = newWebsocketReadyPromise
    }

    (websocket, websocketReadyPromise)
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

  private def logLine(line: String): Unit = println(s"  [WebSocket] $line")
}
