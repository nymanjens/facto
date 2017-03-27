package flux.react.uielements

import flux.stores.entries.GroupedTransactions
import flux.react.ReactVdomUtils.{^^, <<}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import common.GuavaReplacement.Iterables.getOnlyElement

import scala.collection.immutable.Seq

object DescriptionWithEntryCount {
  private case class Props(entry: GroupedTransactions)
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      val entry = props.entry
      val tagIndications = entry.tags.map(tag =>
        <.span(^^.classes("label", s"label-${tag.bootstrapClassSuffix}"), tag.name)
      )

      if (entry.transactions.size == 1) {
        <.span(
          tagIndications,
          entry.descriptions
        )
      } else {
        UpperRightCorner(cornerContent = s"(${entry.transactions.size})")(centralContent =
          tagIndications,
          entry.descriptions.mkString(", ")
        )
      }
    }
    ).build

  def apply(entry: GroupedTransactions): ReactElement = {
    component(Props(entry))
  }
}
