package app.common.money

import java.lang.Math.abs
import hydro.common.GuavaReplacement.DoubleMath.roundToLong
import hydro.common.time.LocalDateTime
import hydro.common.GuavaReplacement.Splitter

import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * Base class for any type that represents an amount of money.
 *
 * Although this has a `currency` method, implementations are allowed to pick a single fixed currency.
 */
trait Money {

  def cents: Long
  def currency: Currency

  final def formatFloat: String = Money.centsToFloatString(cents)
  final def toDouble: Double = cents / 100.0

  final def withDate(date: LocalDateTime): DatedMoney = {
    DatedMoney(cents, currency, date)
  }

  final def isZero: Boolean = cents == 0
  final def nonZero: Boolean = cents != 0

  override def toString = {
    val nonBreakingSpace = "\u00A0"
    s"${currency.symbol}$nonBreakingSpace${Money.centsToFloatString(cents)}"
  }
}

object Money {

  def centsToFloatString(cents: Long): String = {
    val sign = if (cents < 0) "-" else ""
    val centsPart = abs(cents % 100)
    val integerPart = {
      val positiveInteger = roundToLong(abs(cents) / 100)
      if (positiveInteger < 1000) {
        // Optimization for most common case that needs no special treatment
        positiveInteger.toString
      } else {
        positiveInteger.toString.reverseIterator.grouped(3).map(_.mkString("")).mkString(",").reverse
      }
    }
    "%s%s.%02d".format(sign, integerPart, centsPart)
  }

  def floatToCents(float: Double): Long =
    (float.toDouble * 100).round

  /**
   * Parses a string representing an amount of money in the floating point format (without currency)
   * to its number of cents.
   *
   * This method is lenient in its input. It allows both the point or the comma as decimal delimiter. It
   * detects the comma as separator of thousands. It also allows the 'k' suffix which represents x1000.
   *
   * Examples:
   *   - floatStringToCents("1,234") = Some(123400)
   *   - floatStringToCents("1 234") = Some(123400)
   *   - floatStringToCents("1,23") = Some(123)
   *   - floatStringToCents("1.23") = Some(123)
   *   - floatStringToCents("1.23k") = Some(123000)
   *   - floatStringToCents("1.23M") = Some(123000000)
   */
  def floatStringToCents(string: String): Option[Long] = tryFloatStringToCents(string).toOption

  def tryFloatStringToCents(string: String): Try[Long] = {
    def parseWithoutSignOrMetricPrefix(string: String): Try[Long] = {
      def parseCents(string: String): Long = {
        string match {
          case _ if string.length == 0 => 0
          case _ if string.length == 1 => string.toLong * 10
          case _ if string.length == 2 => string.toLong
          case _                       => throw new Exception(s"string.size == ${string.length}")
        }
      }
      def parseNonCents(string: String): Long = {
        if (string.isEmpty) {
          0
        } else {
          string.toLong * 100
        }
      }
      def containsOnlyDigitsAndDelimiters(string: String): Boolean = {
        val delimiters = Set(',', '.')
        def validChar(c: Char) = c.isDigit || (delimiters contains c)
        string.filter(!validChar(_)).isEmpty
      }

      if (!containsOnlyDigitsAndDelimiters(string)) {
        Failure(new Exception(s"string '$string' contains illegal characters"))
      } else if (!string.exists(_.isDigit)) {
        Failure(new Exception(s"string '$string' contains no digits"))
      } else {
        val parts = Splitter.on('.').split(string).flatMap(Splitter.on(',').split)

        if (parts.size <= 1) {
          require(parts.size == 1)
          Success(parseNonCents(parts.mkString("")))
        } else {
          if (parts.drop(1).dropRight(1).exists(_.length != 3)) {
            Failure(new Exception(s"Middle parts of separated amounts need to have length 3"))
          } else if (parts(0).isEmpty && parts(1).length > 2) {
            Failure(new Exception(s"Only the decimal part can start with a dot"))
          } else if (parts.last.length > 3) {
            Failure(new Exception(s"Last part of separated amounts needs to have length 3"))
          } else if (parts.last.length < 3) {
            Success(parseNonCents(parts.dropRight(1).mkString("")) + parseCents(parts.last))
          } else {
            require(parts.last.length == 3)
            Success(parseNonCents(parts.mkString("")))
          }
        }
      }
    }

    def detectAndRemoveSign(string: String): (Boolean, String) = {
      def removePlusSignPrefix(s: String): String = {
        if (s.startsWith("+")) s.substring(1) else s
      }
      val isNegative = string.startsWith("-")
      (isNegative, if (isNegative) string.substring(1) else removePlusSignPrefix(string))
    }

    def detectAndRemoveMetricPrefix(string: String): (Int, String) = {
      val metricPrefixToDecimal = Map('k' -> 1000, 'M' -> 1000000)
      val metricPrefix =
        metricPrefixToDecimal.keys.find(metricPrefix => string endsWith metricPrefix.toString)

      if (metricPrefix.isDefined) {
        (metricPrefixToDecimal(metricPrefix.get), string.substring(0, string.length - 1))
      } else {
        (1, string)
      }
    }

    val (isNegative, stringWithoutSign) = detectAndRemoveSign(cleanCurrencyString(string))
    val (decimal, stringWithoutSignOrMetricPrefix) = detectAndRemoveMetricPrefix(stringWithoutSign)
    val factor = (if (isNegative) -1 else 1) * decimal
    parseWithoutSignOrMetricPrefix(stringWithoutSignOrMetricPrefix) map (cents => factor * cents)
  }

  private def cleanCurrencyString(str: String): String = {
    var result = str.trim
    for (characterToRemove <- " $€£¥₹") {
      result = result.replace(characterToRemove.toString, "")
    }
    result
  }
}
