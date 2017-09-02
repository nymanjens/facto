package jsfacades

import common.GuavaReplacement.Iterables.getOnlyElement
import common.ScalaUtils
import jsfacades.Loki.Sorting.KeyWithDirection
import org.scalajs.dom.raw.KeyboardEvent

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSGlobal
import scala2js.Converters._
import scala2js.Scala2Js

@JSGlobal("Mousetrap")
@js.native
object Mousetrap extends js.Object {

  def bindGlobal(key: String, callback: js.Function1[KeyboardEvent, Unit]): Unit = js.native
}
