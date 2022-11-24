package app.flux.react.uielements

import app.common.accounting.TemplateMatcher

final class Module(implicit
    templateMatcher: TemplateMatcher
) {
  implicit lazy val descriptionWithEntryCount: DescriptionWithEntryCount = new DescriptionWithEntryCount
}
