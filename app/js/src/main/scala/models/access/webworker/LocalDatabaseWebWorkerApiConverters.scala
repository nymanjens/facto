package models.access.webworker

import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation._

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala2js.Scala2Js

private[webworker] object LocalDatabaseWebWorkerApiConverters {

  implicit object WriteOperationConverter extends Scala2Js.Converter[WriteOperation] {
    private val insertNumber: Int = 1
    private val updateNumber: Int = 2
    private val removeNumber: Int = 3
    private val clearNumber: Int = 4
    private val saveDatabaseNumber: Int = 5

    override def toJs(operation: WriteOperation) = {
      operation match {
        case Insert(collectionName, obj) => js.Array[js.Any](insertNumber, collectionName, obj)
        case Update(collectionName, obj) => js.Array[js.Any](updateNumber, collectionName, obj)
        case Remove(collectionName, id)  => js.Array[js.Any](removeNumber, collectionName, id)
        case Clear(collectionName)       => js.Array[js.Any](clearNumber, collectionName)
        case SaveDatabase                => js.Array[js.Any](saveDatabaseNumber)
      }
    }

    override def toScala(value: js.Any) = {
      val seq = value.asInstanceOf[js.Array[js.Any]].toVector
      val firstElement = seq(0).asInstanceOf[Int]

      (firstElement, seq) match {
        case (`insertNumber`, Seq(_, collectionName, obj)) =>
          Insert(collectionName.asInstanceOf[String], obj.asInstanceOf[js.Dictionary[js.Any]])
        case (`updateNumber`, Seq(_, collectionName, obj)) =>
          Update(collectionName.asInstanceOf[String], obj.asInstanceOf[js.Dictionary[js.Any]])
        case (`removeNumber`, Seq(_, collectionName, id)) =>
          Remove(collectionName.asInstanceOf[String], id)
        case (`clearNumber`, Seq(_, collectionName)) => Clear(collectionName.asInstanceOf[String])
        case (`saveDatabaseNumber`, Seq(_))          => SaveDatabase
      }
    }
  }
}
