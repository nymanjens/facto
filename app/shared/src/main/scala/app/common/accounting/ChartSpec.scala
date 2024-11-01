package app.common.accounting

import app.common.accounting.ChartSpec.Line
import hydro.common.GuavaReplacement.Splitter
import hydro.common.ScalaUtils

import scala.collection.immutable.Seq

case class ChartSpec(
    lines: Seq[Line],
    correctForInflation: Boolean,
) {
  def withAddedEmptyLine: ChartSpec = {
    copy(lines = lines :+ Line.empty)
  }

  def withRemovedLine(index: Int): ChartSpec = {
    val mutableLines = lines.toBuffer
    mutableLines.remove(index)
    copy(lines = mutableLines.toVector)
  }

  def modified(index: Int, modification: Line => Line) = {
    copy(lines = lines.updated(index, modification(lines(index))))
  }

  def stringify: String = {
    val inflationSuffix = if (correctForInflation) "+I" else "-I"
    lines.map(_.stringify).mkString(String.valueOf(ChartSpec.lineDelimiter)) + inflationSuffix
  }
}
object ChartSpec {
  def singleEmptyLine() =
    ChartSpec(lines = Seq(Line.empty), correctForInflation = false)

  private val lineDelimiter = '~'

  def parseStringified(string: String): ChartSpec = {
    val correctForInflation = if (string.endsWith("+I")) {
      true
    } else if (string.endsWith("-I")) {
      false
    } else {
      throw new AssertionError(string)
    }
    val stringRemainder = string.substring(0, string.length - 2)

    val lines = Splitter.on(lineDelimiter).split(stringRemainder).map(Line.parseStringified)
    if (lines.nonEmpty) ChartSpec(lines, correctForInflation)
    else ChartSpec.singleEmptyLine().copy(correctForInflation = correctForInflation)
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
}
