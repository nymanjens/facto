package app.api

import java.nio.ByteBuffer

import app.api.Picklers._

/**
  * Picklable combination of method name (= path) and method arguments, representing a single call
  * to a ScalaJS API method.
  */
case class ScalaJsApiRequest(path: String, args: Map[String, ByteBuffer])
