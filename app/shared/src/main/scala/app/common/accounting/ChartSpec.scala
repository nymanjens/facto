package app.common.accounting

import app.common.accounting.ChartSpec.Line
import hydro.common.GuavaReplacement.Splitter

import scala.collection.immutable.Seq

case class ChartSpec(lines: Seq[Line]) {
  def withAddedEmptyLine: ChartSpec = {
    ChartSpec(lines :+ Line.empty)
  }

  def withRemovedLine(index: Int): ChartSpec = {
    val mutableLines = lines.toBuffer
    mutableLines.remove(index)
    ChartSpec(mutableLines.toVector)
  }

  def modified(index: Int, modification: Line => Line) = {
    ChartSpec(lines.updated(index, modification(lines(index))))
  }

  def stringify: String = {
    lines.map(_.stringify).mkString(String.valueOf(ChartSpec.lineDelimiter))
  }
}
object ChartSpec {
  val singleEmptyLine = ChartSpec(lines = Seq(Line.empty))

  private val lineDelimiter = '~'

  def parseStringified(string: String): ChartSpec = {
    val lines = Splitter.on(lineDelimiter).split(string).map(Line.parseStringified)
    if (lines.nonEmpty) ChartSpec(lines) else ChartSpec.singleEmptyLine
  }

  case class Line(
      name: String,
      query: String,
      inverted: Boolean,
      cumulative: Boolean,
  ) {
    def toggleInverted: Line = copy(inverted = !inverted)
    def toggleCumulative: Line = copy(cumulative = !cumulative)

    def stringify: String = {
      def stripDelimiterCharacters(s: String): String = {
        s.replace(String.valueOf(Line.delimiter), "").replace(String.valueOf(ChartSpec.lineDelimiter), "")
      }
      s"${if (inverted) "I" else "_"}${if (cumulative) "C" else "_"}" +
        s"${stripDelimiterCharacters(name)}${Line.delimiter}${stripDelimiterCharacters(query)}"
    }
  }
  object Line {
    val empty: Line = Line(name = "", query = "", inverted = false, cumulative = false)

    private val delimiter = '^'

    def parseStringified(string: String): Line = {
      val Seq(name, query) = Splitter.on(delimiter).split(string.substring(2))
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
      )
    }
  }
}
