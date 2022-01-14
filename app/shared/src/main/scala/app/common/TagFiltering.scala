package app.common

object TagFiltering {
  def normalize(tag: String): String = {
    tag.toLowerCase.filter(_.isLetterOrDigit)
  }
}
