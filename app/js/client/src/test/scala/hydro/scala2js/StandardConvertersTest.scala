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
      "Produces ordered results" - {
        val lower = Scala2Js.toJs(999L).asInstanceOf[String]
        val higher = Scala2Js.toJs(1000L).asInstanceOf[String]
        (lower < higher) ==> true
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
  }
}
