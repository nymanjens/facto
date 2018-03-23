package models.access.webworker

import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation._

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala2js.Scala2Js

private[webworker] object LocalDatabaseWebWorkerApiConverters {

  implicit object WriteOperationConverter extends Scala2Js.Converter[WriteOperation] {
    private val insertNumber: Int = 1
    private val findAndRemoveNumber: Int = 2
    private val clearNumber: Int = 3
    private val saveDatabaseNumber: Int = 4

    override def toJs(operation: WriteOperation) = {
      operation match {
        case Insert(collectionName, obj) => js.Array[js.Any](insertNumber, collectionName, obj)
        case FindAndRemove(collectionName, fieldName, fieldValue) =>
          js.Array[js.Any](findAndRemoveNumber, collectionName, fieldName, fieldValue)
        case Clear(collectionName) => js.Array[js.Any](clearNumber, collectionName)
        case SaveDatabase          => js.Array[js.Any](saveDatabaseNumber)
      }
    }

    override def toScala(value: js.Any) = {
      val seq = value.asInstanceOf[js.Array[js.Any]].toVector
      val firstElement = seq(0).asInstanceOf[Int]

      (firstElement, seq) match {
        case (`insertNumber`, Seq(_, collectionName, obj)) =>
          Insert(collectionName.asInstanceOf[String], obj.asInstanceOf[js.Dictionary[js.Any]])
        case (`findAndRemoveNumber`, Seq(_, collectionName, fieldName, fieldValue)) =>
          FindAndRemove(collectionName.asInstanceOf[String], fieldName.asInstanceOf[String], fieldValue)
        case (`clearNumber`, Seq(_, collectionName)) => Clear(collectionName.asInstanceOf[String])
        case (`saveDatabaseNumber`, Seq(_))          => SaveDatabase
      }
    }
  }
}
