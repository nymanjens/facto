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

final class DescriptionWithEntryCount(
    implicit templateMatcher: TemplateMatcher,
) {
  private case class Props(entry: GroupedTransactions)
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      val entry = props.entry

      val maybeTemplateIcon: Option[VdomNode] = templateMatcher.getMatchingTemplate(entry.transactions) map {
        template =>
          <.i(^.key := "template-icon", ^.className := template.iconClass)
      }
      val tagIndications: Seq[VdomNode] =
        entry.tags
          .map(tag => Bootstrap.Label(BootstrapTags.toStableVariant(tag))(^.key := tag, tag): VdomNode)
      val centralContent = joinWithSpaces(maybeTemplateIcon, tagIndications, entry.description)

      if (entry.transactions.size == 1) {
        centralContent
      } else {
        UpperRightCorner(cornerContent = s"(${entry.transactions.size})")(centralContent)
      }
    })
    .build

  def apply(entry: GroupedTransactions): VdomElement = {
    component(Props(entry))
  }

  private def joinWithSpaces(option: Option[VdomNode], seq: Seq[VdomNode], node: VdomNode): VdomArray = {
    <<.joinWithSpaces(option.toSeq ++ seq :+ node)
  }
}
