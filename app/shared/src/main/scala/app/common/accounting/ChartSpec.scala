package app.common.accounting

import app.common.accounting.ChartSpec.Line

import scala.collection.immutable.Seq

case class ChartSpec(lines: Seq[Line]) {
  def withAddedEmptyLine: ChartSpec = {
    ChartSpec(lines :+ Line())
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
    lines.map(_.stringify).mkString(ChartSpec.lineDelimiter)
  }
}
object ChartSpec {
  val singleEmptyLine = ChartSpec(lines = Seq(Line()))

  private val lineDelimiter = "~~"

  def parseStringified(string: String): ChartSpec = {
    val lines = string.split(lineDelimiter).filter(_.nonEmpty).map(Line.parseStringified).toVector
    if (lines.nonEmpty) ChartSpec(lines) else ChartSpec.singleEmptyLine
  }

  case class Line(
      query: String = "",
      inverted: Boolean = false,
      cumulative: Boolean = false,
  ) {
    def toggleInverted: Line = copy(inverted = !inverted)
    def toggleCumulative: Line = copy(cumulative = !cumulative)

    def stringify: String = {
      s"${if (inverted) "I" else "_"}${if (cumulative) "C" else "_"}$query"
    }
  }
  object Line {
    def parseStringified(string: String): Line = {
      Line(
        query = string.substring(2),
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
