package hydro.models.access.webworker

import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WorkerResponse
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.access.webworker.LocalDatabaseWebWorkerApiConverters._
import hydro.scala2js.Scala2Js
import utest._

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls
import scala.scalajs.js

import app.common.testing.TestObjects._

object LocalDatabaseWebWorkerApiConvertersTest extends TestSuite {

  private val testObj: js.Dictionary[js.Any] = js.Dictionary("a" -> 1, "b" -> "2")
  private val testObj2: js.Dictionary[js.Any] = js.Dictionary("a" -> 3, "b" -> "4")

  override def tests = TestSuite {
    "WriteOperationConverter" - {
      "Insert" - { testForwardAndBackward[WriteOperation](WriteOperation.Insert("test", testObj)) }
      "Update" - {
        testForwardAndBackward[WriteOperation](
          WriteOperation.Update("test", testObj, abortUnlessExistingValueEquals = js.undefined))
      }
      "Update with abortUnlessExistingValueEquals" - {
        testForwardAndBackward[WriteOperation](
          WriteOperation.Update("test", testObj, abortUnlessExistingValueEquals = testObj2))
      }
      "Remove" - { testForwardAndBackward[WriteOperation](WriteOperation.Remove("test", 1928371023123987L)) }
      "Clear" - { testForwardAndBackward[WriteOperation](WriteOperation.RemoveCollection("test")) }
      "AddCollection" - {
        testForwardAndBackward[WriteOperation](
          WriteOperation.AddCollection(
            collectionName = "test",
            uniqueIndices = Seq("id"),
            indices = Seq("code"),
            broadcastWriteOperations = true,
          ))
      }
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

    "WorkerResponseConverter" - {
      "Failed" - { testForwardAndBackward[WorkerResponse](WorkerResponse.Failed("test")) }
      "MethodReturnValue" - {
        testForwardAndBackward[WorkerResponse](WorkerResponse.MethodReturnValue(testObj))
      }
      "BroadcastedWriteOperations" - {
        testForwardAndBackward[WorkerResponse](
          WorkerResponse.BroadcastedWriteOperations(
            Seq(WriteOperation.Update("test", testObj, abortUnlessExistingValueEquals = js.undefined))))
      }
    }
  }

  private def testForwardAndBackward[T: Scala2Js.Converter](value: T): Unit = {
    val jsValue = Scala2Js.toJs(value)
    val newValue = Scala2Js.toScala[T](jsValue)
    newValue ==> value
  }
}
