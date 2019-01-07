package hydro.common.testing

import hydro.common.I18n

import scala.collection.mutable

final class FakeI18n extends I18n {

  private val mappings: mutable.Map[String, String] = mutable.Map()

  override def apply(key: String, args: Any*): String =
    mappings.getOrElse(key, key)

  def setMappings(keyToValues: (String, String)*): Unit = {
    for ((key, value) <- keyToValues) {
      mappings.put(key, value)
    }
  }
}
