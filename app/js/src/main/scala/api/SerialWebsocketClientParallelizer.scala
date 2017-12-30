package api

import java.nio.ByteBuffer

import api.Picklers._

import scala.async.Async.async
import scala.concurrent.Future
import scala.collection.immutable.Seq

private[api] final class SerialWebsocketClientParallelizer(websocketPath: String, numWebsockets: Int) {

  private val websocketClients: Seq[SerialWebsocketClient] =
    (0 until numWebsockets).map(_ => new SerialWebsocketClient(websocketPath = websocketPath))

  def sendAndReceive(request: ByteBuffer): Future[ByteBuffer] = {
    websocketClients.minBy(_.backlogSize).sendAndReceive(request)
  }
}
