package tests

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Test framework for unit tests that have to be manually run in a browser. */
object ManualTests {

  def run(): Unit = {
    var nextFuture = Future.successful((): Unit)
    var successCount = 0
    val failingTestNames: mutable.Buffer[String] = mutable.Buffer()

    for (testSuite <- allTestSuites) {
      for (test <- testSuite.tests) {
        nextFuture = nextFuture flatMap { _ =>
          val testName = s"${testSuite.getClass.getSimpleName}: ${test.name}"
          println(s"  [$testName]  Starting test")
          Future.successful((): Unit) flatMap { _ =>
            test.testCode()
          } map { _ =>
            println(s"  [$testName]  Finished test")
            successCount += 1
          } recoverWith {
            case throwable => {
              println(s"  [$testName]  Test failed: $throwable")
              throwable.printStackTrace()
              failingTestNames += testName
              Future.successful((): Unit)
            }
          }
        }
      }
    }
    nextFuture map { _ =>
      println(s"  All tests finished. $successCount succeeded, ${failingTestNames.size} failed")
      if(failingTestNames.nonEmpty) {
        println("")
        println("  Failing tests:")
        for(testName <- failingTestNames) {
        println(s"    - $testName")
        }
      }
    }
  }

  private def allTestSuites: Seq[ManualTestSuite] =
    Seq(new LocalDatabaseImplTest, new LocalDatabaseResultSetTest)

  trait ManualTestSuite {
    def tests: Seq[ManualTest]

    implicit class ArrowAssert[T](lhs: T) {
      def ==>[V](rhs: V) = {
        require(lhs == rhs, s"a ==> b assertion failed: $lhs != $rhs")
      }
    }
  }

  final class ManualTest(val name: String, val testCode: () => Future[Unit])

  object ManualTest {
    def apply(name: String)(testCode: => Future[Unit]): ManualTest = new ManualTest(name, () => testCode)
  }
}
