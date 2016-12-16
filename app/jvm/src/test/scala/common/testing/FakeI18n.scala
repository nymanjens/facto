package common.testing

import common._

final class FakeI18n extends I18n {

  override def apply(key: String, args: Any*): String = key
}
