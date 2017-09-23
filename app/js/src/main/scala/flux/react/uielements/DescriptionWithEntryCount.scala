package flux.react.uielements

import common.accounting.Tags
import flux.react.ReactVdomUtils.^^
import flux.stores.entries.GroupedTransactions
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object DescriptionWithEntryCount {
  private case class Props(entry: GroupedTransactions)
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      val entry = props.entry
      val tagIndications =
        entry.tags
          .map(tag =>
            <.span(^^.classes("label", s"label-${Tags.getBootstrapClassSuffix(tag)}"), ^.key := tag, tag))
          .toVdomArray

      if (entry.transactions.size == 1) {
        <.span(
          tagIndications,
          entry.descriptions.toTagMod
        )
      } else {
        UpperRightCorner(cornerContent = s"(${entry.transactions.size})")(
          centralContent = tagIndications,
          entry.descriptions.mkString(", "))
      }
    })
    .build

  def apply(entry: GroupedTransactions): VdomElement = {
    component(Props(entry))
  }
}
