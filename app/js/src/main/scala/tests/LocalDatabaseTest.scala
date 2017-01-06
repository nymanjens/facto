package models.access

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import tests.ManualTests.{ManualTest, ManualTestSuite}
import common.testing.TestObjects._

import scala.collection.immutable.Seq
import scala.concurrent.Future

object LocalDatabaseTest extends ManualTestSuite {

  override def tests = Seq(
    ManualTest("isEmpty") {
      LocalDatabase.createInMemoryForTests() map { db =>
        db.isEmpty() ==> true
        db.addAll(Seq(testTransactionWithId))
        db.isEmpty() ==> false
      }
    }
  )
}
