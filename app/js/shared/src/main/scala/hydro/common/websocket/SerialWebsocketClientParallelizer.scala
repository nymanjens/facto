package hydro.common.websocket

import java.nio.ByteBuffer

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class SerialWebsocketClientParallelizer(websocketPath: String, numWebsockets: Int) {

  private val websocketClients: Seq[SerialWebsocketClient] =
    (0 until numWebsockets).map(_ => new SerialWebsocketClient(websocketPath = websocketPath))

  def sendAndReceive(request: ByteBuffer): Future[ByteBuffer] = {
    websocketClients.minBy(_.backlogSize).sendAndReceive(request)
  }
}
