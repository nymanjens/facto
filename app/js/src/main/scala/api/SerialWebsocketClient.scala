package api

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import scala.scalajs.js.typedarray.{ArrayBuffer, _}
import java.nio.ByteBuffer

import api.Picklers._
import common.LoggingUtils.logExceptions
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent, _}

import scala.concurrent.{Future, Promise}

private[api] final class SerialWebsocketClient(websocketPath: String) {
  require(!websocketPath.startsWith("/"))

  val websocket = initWebsocket()
  val websocketReadyPromise: Promise[Unit] = Promise()
  var responseMessagePromise: Promise[ByteBuffer] = Promise.successful(ByteBuffer.allocate(0))

  def sendAndReceive(request: ByteBuffer): Future[ByteBuffer] = async {
    // Wait until websocket is initialized and previous request is done
    await(websocketReadyPromise.future)
    await(responseMessagePromise.future)

    logExceptions {
      responseMessagePromise = Promise()

      websocket.send(new Blob(toByteArray(request).toJSArray.asInstanceOf[js.Array[js.Any]]))
    }

    await(responseMessagePromise.future)
  }

  private def initWebsocket(): WebSocket = {
    val websocket = new dom.WebSocket(s"ws://${dom.window.location.host}/$websocketPath")

    websocket.onmessage = (e: MessageEvent) => {
      val bytes = TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])
      onMessageReceived(bytes)
    }
    websocket.onopen = (e: Event) => {
      websocketReadyPromise.success((): Unit)
      logLine("Opened")
    }
    websocket.onerror = (e: ErrorEvent) => {
      responseMessagePromise.tryFailure(new RuntimeException(s"Error has occured: ${e.message}"))
      logLine(s"Error has occured: ${e.message}")
    }
    websocket.onclose = (e: CloseEvent) => logLine("Closed")

    websocket
  }

  private def onMessageReceived(bytes: ByteBuffer): Unit = {
    if (responseMessagePromise.isCompleted) {
      logLine("Warning: Received message without request")
    } else {
      responseMessagePromise.success(bytes)
    }
  }

  private def toByteArray(byteBuffer: ByteBuffer): Array[Byte] = {
    val array = Array.ofDim[Byte](byteBuffer.remaining())
    byteBuffer.get(array)
    array
  }

  private def logLine(line: String): Unit = println(s"  [WebSocket] $line")
}
