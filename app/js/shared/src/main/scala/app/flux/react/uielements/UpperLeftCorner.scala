package app.flux.react.uielements

import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object UpperLeftCorner {
  private case class Props(cornerContent: Seq[TagMod], centralContent: Seq[TagMod])
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) =>
      <.div(
        ^^.classes("upper-left-corner-holder"),
        // Fix for Firefox problem: Background color overwrites
        // the td borders when td has position:relative, so instead
        // a div is added that has position:relative (a relative
        // reference is needed for the upper-left-corner to work).
        props.centralContent.toTagMod,
        <.div(^^.classes("upper-left-corner"), props.cornerContent.toTagMod),
      )
    )
    .build

  def apply(cornerContent: TagMod*)(centralContent: TagMod*): VdomElement = {
    component(Props(cornerContent.toVector, centralContent.toVector))
  }
}
