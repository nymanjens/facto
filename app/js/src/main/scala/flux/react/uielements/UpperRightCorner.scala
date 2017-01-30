package flux.react.uielements

import common.I18n
import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

object UpperRightCorner {
  private case class Props(cornerContent: Seq[TagMod], centralContent: Seq[TagMod])
  private val component = ReactComponentB[Props]("UpperRightCorner")
    .renderP((_, props) =>
      <.div(^^.classes("upper-right-corner-holder"),
        // Fix for Firefox problem: Background color overwrites
        // the td borders when td has position:relative, so instead
        // a div is added that has position:relative (a relative
        // reference is needed for the upper-right-corner to work).

        props.centralContent,
        <.div(^^.classes("upper-right-corner"),
          props.cornerContent
        )
      )
    ).build

  def apply(cornerContent: TagMod*)(centralContent: TagMod*): ReactElement = {
    component(Props(cornerContent.toVector, centralContent.toVector))
  }
}
