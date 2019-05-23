package hydro.flux.react.uielements

import hydro.flux.react.uielements.Bootstrap.Variant

import scala.collection.immutable.Seq
import scala.math.abs

object BootstrapTags {
  private val bootstrapClassSuffixOptions: Seq[Variant] =
    Seq(Variant.primary, Variant.success, Variant.info, Variant.warning, Variant.danger)

  def toStableVariant(tag: String): Variant = {
    val index = abs(tag.hashCode) % bootstrapClassSuffixOptions.size
    bootstrapClassSuffixOptions(index)
  }
}
