package api

import java.nio.ByteBuffer

import api.Picklers._

case class ScalaJsApiRequest(path: String, args: Map[String, ByteBuffer])
