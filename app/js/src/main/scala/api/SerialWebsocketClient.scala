package api

import java.nio.ByteBuffer

import api.Picklers._

import scala.concurrent.Future

private[api] final class SerialWebsocketClient {

  def sendAndReceive(request: ByteBuffer): Future[ByteBuffer] = {
    ???
  }
}
