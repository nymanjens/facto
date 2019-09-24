package hydro.models

import java.lang.Math.abs

import app.common.testing.TestObjects._
import app.models.access.ModelFields
import app.models.user.User
import hydro.common.testing._
import hydro.models.access.ModelField
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityModification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.collection.immutable.Seq
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class UpdatableEntityTest extends HookedSpecification {

  private val fieldA: ModelField.any = ModelFields.User.loginName
  private val fieldB: ModelField.any = ModelFields.User.name
  private val fieldC: ModelField.any = ModelFields.User.passwordHash
  private val testInstantBIncrement = testInstantB plusNanos 1
  private val testInstantCIncrement = testInstantC plusNanos 1
  private val testInstantDIncrement = testInstantD plusNanos 1

  "merge" in {
    "allFieldsUpdated + allFieldsUpdated" in {
      val user1 = randomUser(LastUpdateTime.allFieldsUpdated(testInstantA))
      val user2 = randomUser(LastUpdateTime.allFieldsUpdated(testInstantB))

      UpdatableEntity.merge(user1, user2) mustEqual user2
      UpdatableEntity.merge(user2, user1) mustEqual user2
    }
    "allFieldsUpdated + someFieldsUpdated" in {
      val user1 = randomUser(LastUpdateTime.allFieldsUpdated(testInstantA))
      val user2 = randomUser(LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB), fieldA = "xxx")

      val expectedTime =
        LastUpdateTime(timePerField = Map(fieldA -> testInstantB), otherFieldsTime = Some(testInstantA))
      UpdatableEntity.merge(user1, user2) mustEqual copy(user1, fieldA = "xxx", lastUpdateTime = expectedTime)
      UpdatableEntity.merge(user2, user1) mustEqual copy(user1, fieldA = "xxx", lastUpdateTime = expectedTime)
    }
    "neverUpdated + someFieldsUpdated" in {
      val user1 = randomUser(LastUpdateTime.neverUpdated)
      val user2 = randomUser(LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB), fieldA = "aaa")

      val expectedTime = user2.lastUpdateTime
      UpdatableEntity.merge(user1, user2) mustEqual user2
      UpdatableEntity.merge(user2, user1) mustEqual copy(user1, fieldA = "aaa", lastUpdateTime = expectedTime)
    }
    "someFieldsUpdated + someFieldsUpdated" in {
      val user1 =
        randomUser(LastUpdateTime.someFieldsUpdated(Seq(fieldA, fieldB), testInstantA), fieldA = "aaa")
      val user2 = randomUser(
        LastUpdateTime.someFieldsUpdated(Seq(fieldB, fieldC), testInstantB),
        fieldB = "bbb",
        fieldC = "ccc")

      val expectedTime =
        LastUpdateTime(
          timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantB, fieldC -> testInstantB),
          otherFieldsTime = None)
      UpdatableEntity.merge(user1, user2) mustEqual copy(user2, fieldA = "aaa", lastUpdateTime = expectedTime)
      UpdatableEntity.merge(user2, user1) mustEqual
        copy(user1, fieldB = "bbb", fieldC = "ccc", lastUpdateTime = expectedTime)
    }
    "general + general" in {
      val user1 = randomUser(
        LastUpdateTime(timePerField = Map(fieldA -> testInstantD), otherFieldsTime = Some(testInstantB)),
        fieldA = "aaa")
      val user2 = randomUser(
        LastUpdateTime(
          timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantC),
          otherFieldsTime = Some(testInstantA)),
        fieldB = "bbb")

      val expectedTime = LastUpdateTime(
        timePerField = Map(fieldA -> testInstantD, fieldB -> testInstantC),
        otherFieldsTime = Some(testInstantB))
      val expected = copy(user1, fieldB = "bbb", lastUpdateTime = expectedTime)
      UpdatableEntity.merge(user1, user2) mustEqual expected
      UpdatableEntity.merge(user2, user1) mustEqual expected
    }
  }

  "LastUpdateTime" in {
    "canonicalized" in {
      "neverUpdated" in {
        val time = LastUpdateTime.neverUpdated
        time.canonicalized mustEqual time
      }
      "allFieldsUpdated" in {
        val time = LastUpdateTime.allFieldsUpdated(testInstantA)
        time.canonicalized mustEqual time
      }
      "someFieldsUpdated" in {
        val time = LastUpdateTime.someFieldsUpdated(Seq(fieldA, fieldB), testInstantA)
        time.canonicalized mustEqual time
      }
      "general" in {
        "otherFieldsTime is oldest" in {
          val time =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantC), otherFieldsTime = Some(testInstantA))
          time.canonicalized mustEqual time
        }
        "otherFieldsTime is newest" in {
          LastUpdateTime(timePerField = Map(fieldA -> testInstantA), otherFieldsTime = Some(testInstantB)).canonicalized mustEqual
            LastUpdateTime.allFieldsUpdated(testInstantB)
        }
        "otherFieldsTime is in the middle" in {
          LastUpdateTime(
            timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantC),
            otherFieldsTime = Some(testInstantB)).canonicalized mustEqual
            LastUpdateTime(timePerField = Map(fieldB -> testInstantC), otherFieldsTime = Some(testInstantB))
        }
      }
    }

    "merge" in {
      "forceIncrement = false" in {
        "allFieldsUpdated + allFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.allFieldsUpdated(testInstantB)

          val expected = time2
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "allFieldsUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          val expected =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantB), otherFieldsTime = Some(testInstantA))
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "neverUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.neverUpdated
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          val expected = time2
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "someFieldsUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.someFieldsUpdated(Seq(fieldA, fieldB), testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldB, fieldC), testInstantB)

          val expected =
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantB, fieldC -> testInstantB),
              otherFieldsTime = None)
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "general + general" in {
          val time1 =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantD), otherFieldsTime = Some(testInstantB))
          val time2 = LastUpdateTime(
            timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantC),
            otherFieldsTime = Some(testInstantA))

          val expected = LastUpdateTime(
            timePerField = Map(fieldA -> testInstantD, fieldB -> testInstantC),
            otherFieldsTime = Some(testInstantB))
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
      }
      "forceIncrement = true" in {
        "allFieldsUpdated + allFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.allFieldsUpdated(testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual time2
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime.allFieldsUpdated(testInstantBIncrement)
        }
        "allFieldsUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual
            LastUpdateTime(timePerField = Map(fieldA -> testInstantB), otherFieldsTime = Some(testInstantA))
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime.allFieldsUpdated(testInstantBIncrement)
        }
        "neverUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.neverUpdated
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual time2
        }
        "someFieldsUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.someFieldsUpdated(Seq(fieldA, fieldB), testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldB, fieldC), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantB, fieldC -> testInstantB),
              otherFieldsTime = None)
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField =
                Map(fieldA -> testInstantA, fieldB -> testInstantBIncrement, fieldC -> testInstantB),
              otherFieldsTime = None)
        }
        "general + someFieldsUpdated (no overlap)" in {
          val time1 =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantC), otherFieldsTime = Some(testInstantA))
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldB), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantC, fieldB -> testInstantB),
              otherFieldsTime = Some(testInstantA))
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantC),
              otherFieldsTime = Some(testInstantBIncrement))
        }
        "general + general" in {
          val time1 =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantD), otherFieldsTime = Some(testInstantB))
          val time2 = LastUpdateTime(
            timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantC),
            otherFieldsTime = Some(testInstantA))

          time1.merge(time2, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantDIncrement, fieldB -> testInstantC),
              otherFieldsTime = Some(testInstantBIncrement))
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantD),
              otherFieldsTime = Some(testInstantCIncrement))
        }
      }
    }
  }

  private def randomUser(
      lastUpdateTime: LastUpdateTime,
      fieldA: String = null,
      fieldB: String = null,
      fieldC: String = null,
  ): User = {

    def randomString(prefix: String): String = s"${prefix}_${abs(Random.nextInt()).toString.substring(0, 4)}"
    testUser.copy(
      idOption = Some(EntityModification.generateRandomId()),
      lastUpdateTime = lastUpdateTime,
      loginName = Option(fieldA) getOrElse randomString("fieldA"),
      name = Option(fieldB) getOrElse randomString("fieldB"),
      passwordHash = Option(fieldC) getOrElse randomString("fieldC"),
    )
  }
  private def copy(
      user: User,
      lastUpdateTime: LastUpdateTime = null,
      fieldA: String = null,
      fieldB: String = null,
      fieldC: String = null,
  ): User = {
    user.copy(
      loginName = Option(fieldA) getOrElse user.loginName,
      name = Option(fieldB) getOrElse user.name,
      passwordHash = Option(fieldC) getOrElse user.passwordHash,
      lastUpdateTime = lastUpdateTime,
    )
  }
}
