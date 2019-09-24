package hydro.common

import scala.annotation.tailrec
import scala.collection.immutable.Seq

case class OrderToken(parts: List[Int]) extends Ordered[OrderToken] {
  require(parts.nonEmpty)
  require(
    parts.tail.isEmpty || parts.last != OrderToken.middleValue,
    s"Redundant ${OrderToken.middleValue} at end of $parts")

  override def compare(that: OrderToken): Int = {
    @tailrec
    def innerCompare(parts1: List[Int], parts2: List[Int]): Int = (parts1, parts2) match {
      case (Nil, Nil)                                         => 0
      case (Nil, _)                                           => innerCompare(OrderToken.middleValue :: Nil, parts2)
      case (_, Nil)                                           => innerCompare(parts1, OrderToken.middleValue :: Nil)
      case (head1 :: rest1, head2 :: rest2) if head1 == head2 => innerCompare(rest1, rest2)
      case (head1 :: _, head2 :: _)                           => head1 compare head2
    }
    innerCompare(this.parts, that.parts)
  }

  override def toString = {
    val partsString = parts.map(p => "0x%x".format(p)).mkString(", ")
    s"OrderToken[$partsString]"
  }
}

object OrderToken {
  private val middleValue: Int = 0

  val middle: OrderToken = OrderToken(List(middleValue))

  def middleBetween(lower: Option[OrderToken], higher: Option[OrderToken]): OrderToken = {
    require(
      lower.isEmpty || higher.isEmpty || lower.get <= higher.get,
      s"Not true that lower=$lower <= higher=$higher")

    type HasCarryOver = Boolean
    def middleBetweenWithCarryOver(
        lower: Option[List[Int]],
        higher: Option[List[Int]],
    ): (List[Int], HasCarryOver) = {
      val (lowerHead, lowerNext) = splitHeadAndNext(lower, Int.MinValue)
      val (higherHead, higherNext) = splitHeadAndNext(higher, Int.MaxValue)

      if (lowerHead == Int.MaxValue && higherHead == Int.MinValue) {
        val (resultRest, resultHasCarryOver) = middleBetweenWithCarryOver(lowerNext, higherNext)
        if (resultHasCarryOver) {
          (higherHead.toInt :: resultRest, resultHasCarryOver)
        } else {
          (lowerHead.toInt :: resultRest, resultHasCarryOver)
        }
      } else {
        val intRange = Int.MaxValue.toLong - Int.MinValue.toLong + 1
        val result = (lowerHead + (higherHead + intRange)) / 2
        if (result <= Int.MaxValue) {
          (result.toInt :: Nil, /* HasCarryOver */ false)
        } else {
          ((result - intRange).toInt :: Nil, /* HasCarryOver */ true)
        }
      }
    }

    def innerMiddleBetween(lower: Option[List[Int]], higher: Option[List[Int]]): List[Int] = {
      val (lowerHead, lowerNext) = splitHeadAndNext(lower, Int.MinValue)
      val (higherHead, higherNext) = splitHeadAndNext(higher, Int.MaxValue)

      if (lowerHead == higherHead) {
        lowerHead.toInt :: innerMiddleBetween(lowerNext, higherNext)
      } else if (higherHead - lowerHead >= 2) {
        ((lowerHead + higherHead) / 2).toInt :: Nil
      } else if (higherHead - lowerHead == 1) {
        val (resultRest, resultHasCarryOver) = middleBetweenWithCarryOver(lowerNext, higherNext)
        if (resultHasCarryOver) {
          higherHead.toInt :: resultRest
        } else {
          lowerHead.toInt :: resultRest
        }
      } else {
        throw new IllegalArgumentException(s"lowerHead=$lowerHead, higherHead=$higherHead")
      }
    }

    def doSanityCheck(result: OrderToken): Unit = {
      if (lower.isDefined) {
        require(lower.get <= result, s"Not true that ${lower.get} <= $result")
      }
      if (higher.isDefined) {
        require(higher.get >= result, s"Not true that ${higher.get} >= $result")
      }
    }

    if (lower.isDefined && lower == higher) {
      lower.get
    } else {
      val result = OrderToken(
        removeTrailingMiddleValues(innerMiddleBetween(lower.map(_.parts), higher.map(_.parts))))
      doSanityCheck(result)
      result
    }
  }

  def evenlyDistributedValuesBetween(
      numValues: Int,
      lowerExclusive: Option[OrderToken],
      higherExclusive: Option[OrderToken],
  ): Seq[OrderToken] = {
    val resultBuffer = (for (i <- 0 until numValues) yield null.asInstanceOf[OrderToken]).toBuffer

    def resultFiller(
        resultIndexBaseline: Int = 0,
        numValues: Int,
        lower: Option[OrderToken],
        higher: Option[OrderToken],
    ): Unit = {
      val middle = middleBetween(lower, higher)
      val middleIndexOffset = numValues / 2
      resultBuffer.update(resultIndexBaseline + middleIndexOffset, middle)
      if (middleIndexOffset > 0) {
        resultFiller(
          resultIndexBaseline = resultIndexBaseline,
          numValues = middleIndexOffset,
          lower = lower,
          higher = Some(middle))
      }
      if (middleIndexOffset + 1 < numValues) {
        resultFiller(
          resultIndexBaseline = resultIndexBaseline + middleIndexOffset + 1,
          numValues = numValues - (middleIndexOffset + 1),
          lower = Some(middle),
          higher = higher)
      }
    }
    resultFiller(numValues = numValues, lower = lowerExclusive, higher = higherExclusive)
    resultBuffer.toVector
  }

  private def splitHeadAndNext(
      listOption: Option[List[Int]],
      noneOptionValue: Int,
  ): (Long, Option[List[Int]]) =
    listOption match {
      case None               => (noneOptionValue.toLong, None)
      case Some(Nil)          => (middleValue.toLong, Some(Nil))
      case Some(head :: rest) => (head.toLong, Some(rest))
    }

  private def removeTrailingMiddleValues(list: List[Int]): List[Int] = {
    def inner(list: List[Int]): List[Int] = list match {
      case Nil => Nil
      case `middleValue` :: tail =>
        inner(tail) match {
          case Nil          => Nil
          case trimmmedTail => middleValue :: trimmmedTail
        }
      case head :: tail => head :: inner(tail)
    }

    // Optimization
    if (list.last == middleValue) {
      // Don't remove first middle value
      list.head :: inner(list.tail)
    } else {
      list
    }
  }
}
