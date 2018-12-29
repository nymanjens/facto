package hydro.models.access

import app.common.ScalaUtils
import app.common.ScalaUtils.visibleForTesting

import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._
import hydro.scala2js.Scala2Js
import app.models.access._

@visibleForTesting
sealed trait SingletonKey[V] {
  implicit def valueConverter: Scala2Js.Converter[V]

  def name: String = ScalaUtils.objectName(this)
  override def toString = name
}

@visibleForTesting
object SingletonKey {
  abstract class StringSingletonKey extends SingletonKey[String] {
    override val valueConverter = implicitly[Scala2Js.Converter[String]]
  }

  object NextUpdateTokenKey extends StringSingletonKey
  object VersionKey extends StringSingletonKey
}
