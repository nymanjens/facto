package app.common.accounting

import app.common.accounting.ChartSpec.AggregationPeriod
import app.common.accounting.ChartSpec.Line
import hydro.common.GuavaReplacement.Splitter
import hydro.common.ScalaUtils

import scala.collection.immutable.Seq

case class ChartSpec(
    lines: Seq[Line],
    correctForInflation: Boolean,
    aggregationPeriod: AggregationPeriod,
) {
  def withAddedEmptyLine: ChartSpec = {
    copy(lines = lines :+ Line.empty)
  }

  def withRemovedLine(index: Int): ChartSpec = {
    val mutableLines = lines.toBuffer
    mutableLines.remove(index)
    copy(lines = mutableLines.toVector)
  }

  def modified(index: Int, modification: Line => Line): ChartSpec = {
    copy(lines = lines.updated(index, modification(lines(index))))
  }

  def stringify: String = {
    val inflationSuffix = if (correctForInflation) "+I" else "-I"
    val periodSuffix = aggregationPeriod match {
      case AggregationPeriod.Month => "<>M"
      case AggregationPeriod.Year  => "<>Y"
    }
    lines.map(_.stringify).mkString(String.valueOf(ChartSpec.lineDelimiter)) + inflationSuffix + periodSuffix
  }
}
object ChartSpec {
  def singleEmptyLine(): ChartSpec =
    ChartSpec(
      lines = Seq(Line.empty),
      correctForInflation = false,
      aggregationPeriod = AggregationPeriod.Month,
    )

  private val lineDelimiter = '~'

  def parseStringified(string: String): ChartSpec = {
    var stringRemainder = string
    val aggregationPeriod =
      if (stringRemainder.contains("<>")) {
        val suffix = stringRemainder.substring(stringRemainder.length - 3)
        stringRemainder = stringRemainder.substring(0, stringRemainder.length - 3)
        suffix match {
          case "<>M" => AggregationPeriod.Month
          case "<>Y" => AggregationPeriod.Year
        }
      } else {
        AggregationPeriod.Month
      }
    val correctForInflation = {
      val suffix = stringRemainder.substring(stringRemainder.length - 2)
      stringRemainder = stringRemainder.substring(0, stringRemainder.length - 2)
      suffix match {
        case "+I" => true
        case "-I" => false
      }
    }

    val lines = Splitter.on(lineDelimiter).split(stringRemainder).map(Line.parseStringified)
    if (lines.nonEmpty) {
      ChartSpec(lines, correctForInflation, aggregationPeriod)
    } else {
      ChartSpec
        .singleEmptyLine()
        .copy(correctForInflation = correctForInflation, aggregationPeriod = aggregationPeriod)
    }
  }

  case class Line(
      name: String,
      query: String,
      inverted: Boolean = false,
      cumulative: Boolean = false,
      showBars: Boolean = false,
  ) {
    def toggleInverted: Line = copy(inverted = !inverted)
    def toggleCumulative: Line = copy(cumulative = !cumulative)
    def toggleShowBars: Line = copy(showBars = !showBars)

    def stringify: String = {
      def stripDelimiterCharacters(s: String): String = {
        s.replace(String.valueOf(Line.delimiter), "").replace(String.valueOf(ChartSpec.lineDelimiter), "")
      }
      s"${if (inverted) "I" else "_"}${if (cumulative) "C" else "_"}${if (showBars) "B" else "_"}" +
        s"${stripDelimiterCharacters(name)}${Line.delimiter}${stripDelimiterCharacters(query)}"
    }
  }
  object Line {
    val empty: Line = Line(name = "", query = "")

    private val delimiter = '^'

    def parseStringified(string: String): Line = {
      val Seq(name, query) = Splitter.on(delimiter).split(string.substring(3))
      Line(
        name = name,
        query = query,
        inverted = string.charAt(0) match {
          case 'I' => true
          case '_' => false
        },
        cumulative = string.charAt(1) match {
          case 'C' => true
          case '_' => false
        },
        showBars = string.charAt(2) match {
          case 'B' => true
          case '_' => false
        },
      )
    }
  }

  sealed trait AggregationPeriod
  object AggregationPeriod {
    val all: Seq[AggregationPeriod] = Seq(Month, Year)

    object Month extends AggregationPeriod
    object Year extends AggregationPeriod
  }
}
