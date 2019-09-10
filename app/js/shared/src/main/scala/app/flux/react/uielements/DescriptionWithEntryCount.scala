package app.flux.react.uielements

import app.common.accounting.TemplateMatcher
import hydro.common.Tags
import app.flux.stores.entries.GroupedTransactions
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.BootstrapTags
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class DescriptionWithEntryCount(
    implicit templateMatcher: TemplateMatcher,
) {
  private case class Props(entry: GroupedTransactions)
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      val entry = props.entry
      val tagIndications =
        <<.joinWithSpaces(
          entry.tags
            .map(tag => Bootstrap.Label(BootstrapTags.toStableVariant(tag))(^.key := tag, tag)) ++
            // Add empty span to force space after non-empty label list
            Seq(<.span(^.key := "empty-span-for-space")))
//      val maybeTemplateIcon = <<.ifDefined() {
//
//      }

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
