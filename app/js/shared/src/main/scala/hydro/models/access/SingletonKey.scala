package hydro.models.access

import hydro.common.ScalaUtils
import hydro.common.ScalaUtils.visibleForTesting
import hydro.scala2js.Scala2Js
import hydro.scala2js.StandardConverters._

@visibleForTesting
private[access] sealed trait SingletonKey[V] {
  implicit def valueConverter: Scala2Js.Converter[V]

  def name: String = ScalaUtils.objectName(this)
  override def toString = name
}

@visibleForTesting
private[access] object SingletonKey {
  abstract class StringSingletonKey extends SingletonKey[String] {
    override val valueConverter = implicitly[Scala2Js.Converter[String]]
  }

  object NextUpdateTokenKey extends StringSingletonKey
  object VersionKey extends StringSingletonKey
}
