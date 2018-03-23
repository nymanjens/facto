package models.access.webworker

import models.Entity
import models.access.DbQuery
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js
import scala2js.Scala2Js

object LocalDatabaseWebWorkerApiConverters {

  implicit object WriteOperationConverter extends Scala2Js.Converter[WriteOperation] {
    override def toJs(operation: WriteOperation) = {
      ???
    }
    override def toScala(value: js.Any) = ???
  }
}
