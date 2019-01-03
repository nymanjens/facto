package hydro.models.access.webworker

import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import hydro.scala2js.Scala2Js
import utest._

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls
import scala.scalajs.js

object LocalDatabaseWebWorkerApiConvertersTest extends TestSuite {

  private val testObj: js.Dictionary[js.Any] = js.Dictionary("a" -> 1, "b" -> "2")

  override def tests = TestSuite {
    "WriteOperationConverter" - {
      "Insert" - { testForwardAndBackward[WriteOperation](WriteOperation.Insert("test", testObj)) }
      "Update" - { testForwardAndBackward[WriteOperation](WriteOperation.Update("test", testObj)) }
      "Remove" - { testForwardAndBackward[WriteOperation](WriteOperation.Remove("test", "192837")) }
      "Clear" - { testForwardAndBackward[WriteOperation](WriteOperation.RemoveCollection("test")) }
      "AddCollection" - {
        testForwardAndBackward[WriteOperation](WriteOperation.AddCollection("test", Seq("id"), Seq("code")))
      }
      "SaveDatabase" - { testForwardAndBackward[WriteOperation](WriteOperation.SaveDatabase) }
    }

    "LokiQueryConverter" - {
      testForwardAndBackward(LokiQuery(collectionName = "test"))
      testForwardAndBackward(
        LokiQuery(
          collectionName = "test",
          filter = Some(testObj),
          sorting = Some(js.Array(js.Array[js.Any]("xx", 12))),
          limit = Some(1238)))
    }
  }

  private def testForwardAndBackward[T: Scala2Js.Converter](value: T): Unit = {
    val jsValue = Scala2Js.toJs(value)
    val newValue = Scala2Js.toScala[T](jsValue)
    newValue ==> value
  }
}
