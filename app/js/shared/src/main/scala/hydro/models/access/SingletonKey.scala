package hydro.models.access

import java.time.Instant

import app.scala2js.AppConverters
import hydro.common.ScalaUtils
import hydro.common.Annotations.visibleForTesting
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.UpdatableEntity
import hydro.scala2js.Scala2Js
import hydro.scala2js.Scala2Js.Converter
import hydro.scala2js.StandardConverters._

import scala.scalajs.js

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

  object DbStatusKey extends SingletonKey[DbStatus] {
    override val valueConverter: Converter[DbStatus] = DbStatusConverter

    private object DbStatusConverter extends Converter[DbStatus] {
      private val populatingNumber: Int = 1
      private val readyNumber: Int = 2

      override def toJs(value: DbStatus) = {
        val result = js.Array[js.Any]()

        value match {
          case DbStatus.Populating(startTime) =>
            result.push(populatingNumber)
            result.push(Scala2Js.toJs(startTime))
          case DbStatus.Ready =>
            result.push(readyNumber)
        }

        result
      }

      override def toScala(value: js.Any) = {
        val array = value.asInstanceOf[js.Array[js.Any]]
        val typeNumber = Scala2Js.toScala[Int](array.shift())

        array.toVector match {
          case Vector(startTime) if typeNumber == populatingNumber =>
            DbStatus.Populating(Scala2Js.toScala[Instant](startTime))
          case Vector() if typeNumber == readyNumber =>
            DbStatus.Ready
        }
      }
    }
  }

  sealed trait DbStatus
  object DbStatus {
    case class Populating(startTime: Instant) extends DbStatus
    object Ready extends DbStatus
  }
}
