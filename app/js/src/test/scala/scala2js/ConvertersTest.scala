package scala2js

import java.time.Month.MARCH

import common.time.LocalDateTime
import models.manager.EntityType
import utest._
import scala.collection.immutable.Seq
import scala.scalajs.js

import scala2js.Converters._

object ConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  def tests = TestSuite {
    'entityTypeToConverter {
      entityTypeToConverter(EntityType.TransactionType) ==> TransactionConverter
    }

    'seqConverter {
      val seq = Seq(1, 2)
      val jsValue = Scala2Js.toJs(seq)
      assert(jsValue.isInstanceOf[js.Array[_]])
      Scala2Js.toScala[Seq[Int]](jsValue) ==> seq
    }

    'LocalDateTimeConverter {
      val jsValue = Scala2Js.toJs(dateTime)
      assert(jsValue.isInstanceOf[Int])
      Scala2Js.toScala[LocalDateTime](jsValue) ==> dateTime
    }

    'TransactionConverter {
      // TODO
    }
  }
}
