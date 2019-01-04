package hydro.common.testing

import hydro.common.PlayI18n

import scala.collection.mutable

final class FakePlayI18n extends PlayI18n {

  private val mappings: mutable.Map[String, String] = mutable.Map()

  override def apply(key: String, args: Any*): String =
    mappings.getOrElse(key, key)

  override def allI18nMessages = mappings.toMap

  def setMappings(keyToValues: (String, String)*): Unit = {
    for ((key, value) <- keyToValues) {
      mappings.put(key, value)
    }
  }
}
