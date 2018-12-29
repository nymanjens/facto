package app.flux.react.uielements

import common.accounting.Tags
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import app.flux.stores.entries.GroupedTransactions
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object DescriptionWithEntryCount {
  private case class Props(entry: GroupedTransactions)
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      val entry = props.entry
      val tagIndications =
        <<.joinWithSpaces(entry.tags
          .map(tag =>
            <.span(^^.classes("label", s"label-${Tags.getBootstrapClassSuffix(tag)}"), ^.key := tag, tag)) ++
          // Add empty span to force space after non-empty label list
          Seq(<.span(^.key := "empty-span-for-space")))

      if (entry.transactions.size == 1) {
        <.span(tagIndications, entry.description)
      } else {
        UpperRightCorner(cornerContent = s"(${entry.transactions.size})")(
          centralContent = tagIndications,
          entry.description)
      }
    })
    .build

  def apply(entry: GroupedTransactions): VdomElement = {
    component(Props(entry))
  }
}
