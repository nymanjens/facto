package common.money

import java.lang.Math.abs

import common.GuavaReplacement.DoubleMath.roundToLong
import common.time.LocalDateTime

import scala.util.{Failure, Try}

/**
  * Base class for any type that represents an amount of money.
  *
  * Although this has a `currency` method, implementations are allowed to pick a single fixed currency.
  */
trait Money {

  def cents: Long
  def currency: Currency

  final def formatFloat: String = Money.centsToFloatString(cents)

  final def withDate(date: LocalDateTime): DatedMoney = {
    DatedMoney(cents, currency, date)
  }

  final def isZero: Boolean = cents == 0
  final def nonZero: Boolean = cents != 0

  override def toString = s"${currency.symbol} ${Money.centsToFloatString(cents)}"
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
    * also detects the comma as separator of thousands.
    *
    * Examples:
    *   - floatStringToCents("1,234") = Some(123400)
    *   - floatStringToCents("1 234") = Some(123400)
    *   - floatStringToCents("1,23") = Some(123)
    *   - floatStringToCents("1.23") = Some(123)
    */
  def floatStringToCents(string: String): Option[Long] = tryFloatStringToCents(string).toOption

  def tryFloatStringToCents(string: String): Try[Long] = {
    def parseWithoutSign(string: String): Try[Long] = {
      def parseCents(string: String): Try[Long] = {
        string match {
          case _ if string.length == 1 => Try(string.toLong * 10)
          case _ if string.length == 2 => Try(string.toLong)
          case _ => Failure(new Exception(s"string.size == ${string.length}"))
        }
      }
      def parseNonCents(string: String): Try[Long] = {
        Try(string.toLong * 100)
      }
      def parseDelimitedBy(delimiter: String, string: String): Try[Long] = {
        require(string contains delimiter)

        val firstPart = string.substring(0, string.indexOf(delimiter))
        val secondPart = string.substring(string.indexOf(delimiter) + 1)
        require(!(firstPart contains delimiter))

        if (secondPart contains delimiter) {
          Failure(new Exception(s"secondPart '$secondPart' contains delimiter '$delimiter'"))
        } else {
          (firstPart, secondPart) match {
            case ("", "") => Failure(new Exception(s"Empty string ($string)"))
            case ("", _) => parseCents(secondPart)
            case (_, "") => parseNonCents(firstPart)
            case _ =>
              for {
                nonCents <- parseNonCents(firstPart)
                cents <- parseCents(secondPart)
              } yield nonCents + cents
          }
        }
      }
      def containsOnlyDigitsAndDelimiters(string: String): Boolean = {
        val delimiters = Set(',', '.')
        def validChar(c: Char) = c.isDigit || (delimiters contains c)
        string.filter(!validChar(_)).isEmpty
      }

      if (!containsOnlyDigitsAndDelimiters(string)) {
        Failure(new Exception(s"string '$string' contains illegal characters"))
      } else if (string contains ".") {
        parseDelimitedBy(".", string.replace(",", ""))
      } else if (string contains ",") {
        val commaParts = string.split(",")
        if (!string.startsWith(",")
            && commaParts.nonEmpty
            && commaParts.tail.map(_.length).toSet == Set(3)) {
          parseNonCents(string.replace(",", ""))
        } else {
          parseDelimitedBy(",", string)
        }
      } else {
        parseNonCents(string)
      }
    }

    def detectAndRemoveSign(string: String): (Boolean, String) = {
      def removePlusSignPrefix(s: String): String = {
        if (s.startsWith("+")) s.substring(1) else s
      }
      val isNegative = string.startsWith("-")
      (isNegative, if (isNegative) string.substring(1) else removePlusSignPrefix(string))
    }

    val stringWithoutSpaces = string.replace(" ", "")
    val (isNegative, stringWithoutSign) = detectAndRemoveSign(stringWithoutSpaces)
    if (isNegative) {
      parseWithoutSign(stringWithoutSign) map (cents => -cents)
    } else {
      parseWithoutSign(stringWithoutSign)
    }
  }
}
