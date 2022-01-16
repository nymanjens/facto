package hydro.scala2js

import java.time.Instant
import java.time.Month.MARCH
import app.common.testing.TestObjects._
import app.models.access.ModelFields
import app.models.user.User
import hydro.common.time.LocalDateTime
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.scala2js.StandardConverters._
import utest._

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import scala.util.Random

object StandardConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  override def tests = TestSuite {
    "fromModelField" - {
      StandardConverters.fromModelField(ModelFields.User.loginName) ==> StandardConverters.StringConverter
      StandardConverters.fromModelField(ModelFields.id[User]) ==> StandardConverters.LongConverter
    }

    "seqConverter" - {
      "produced values" - {
        val seq = Seq(1, 2)
        val jsValue = Scala2Js.toJs(seq)
        assert(jsValue.isInstanceOf[js.Array[_]])
        Scala2Js.toScala[Seq[Int]](jsValue) ==> seq
      }
      "to JS and back" - {
        testToJsAndBack[Seq[String]](Seq("a", "b"))
        testToJsAndBack[Seq[String]](Seq())
      }
    }

    "optionConverter" - {
      testToJsAndBack[Option[String]](Some("x"))
      testToJsAndBack[Option[String]](None)
    }

    "LongConverter" - {
      "to JS and back" - {
        testToJsAndBack[Long](1L)
        testToJsAndBack[Long](0L)
        testToJsAndBack[Long](-1L)
        testToJsAndBack[Long](-12392913292L)
        testToJsAndBack[Long](911427549585351L) // 15 digits, which is the maximal javascript precision
        testToJsAndBack[Long](6886911427549585129L)
        testToJsAndBack[Long](-6886911427549585129L)
      }
      "to JS and back (random numbers)" - {
        for (i <- 1 to 100) yield {
          testToJsAndBack[Long](Random.nextLong)
        }
      }
      "Produces ordered results" - {
        def checkOrdering(lower: Long, higher: Long) = {
          val lowerJs = Scala2Js.toJs(lower).asInstanceOf[String]
          val higherJs = Scala2Js.toJs(higher).asInstanceOf[String]
          (lowerJs < higherJs) ==> true
        }
        "Positive numbers" - {
          checkOrdering(lower = 999, higher = 1000)
          checkOrdering(lower = 0, higher = 1000)
          checkOrdering(lower = 0, higher = 1)
        }
        "Negative numbers" - {
          checkOrdering(lower = -3, higher = -2)
          checkOrdering(lower = -33, higher = -32)
          checkOrdering(lower = -33, higher = -1)
          checkOrdering(lower = -99999, higher = -99)
        }
        "Negative/positive numbers" - {
          checkOrdering(lower = -1, higher = 1)
          checkOrdering(lower = -1, higher = 0)
          checkOrdering(lower = -99999, higher = 0)
          checkOrdering(lower = -1, higher = 99999)
          checkOrdering(lower = -99999, higher = 10)
        }
        "Random numbers" - {
          for (i <- 1 to 100) yield {
            val long = Random.nextLong
            checkOrdering(lower = long, higher = long + 1)
            checkOrdering(lower = long - 1, higher = long)
          }
        }
      }
    }

    "LocalDateTimeConverter" - {
      testToJsAndBack[LocalDateTime](LocalDateTime.of(2022, MARCH, 13, 12, 13))
    }

    "InstantConverter" - {
      testToJsAndBack[Instant](testInstant)
    }

    "FiniteDurationConverter" - {
      testToJsAndBack[FiniteDuration](28.minutes)
    }

    "OrderTokenConverter" - {
      testToJsAndBack(testOrderToken)
    }

    "LastUpdateTimeConverter" - {
      testToJsAndBack(testLastUpdateTime)
    }

    "EntityTypeConverter" - {
      testToJsAndBack[EntityType.any](User.Type)
    }

    "EntityModificationConverter" - {
      "Add" - {
        testToJsAndBack[EntityModification](EntityModification.Add(testUserRedacted))
      }
      "Update" - {
        testToJsAndBack[EntityModification](EntityModification.Update(testUserA))
      }
      "Remove" - {
        testToJsAndBack[EntityModification](EntityModification.Remove[User](19238))
      }
    }
  }

  private def testToJsAndBack[T: Scala2Js.Converter](value: T) = {
    val jsValue = Scala2Js.toJs[T](value)
    val generated = Scala2Js.toScala[T](jsValue)
    generated ==> value

    // Test a second time, to check that `jsValue` is not mutated
    val generated2 = Scala2Js.toScala[T](jsValue)
    generated2 ==> value
  }
}
