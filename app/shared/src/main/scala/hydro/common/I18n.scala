package hydro.common

trait I18n {

  def apply(key: String, args: Any*): String
}
