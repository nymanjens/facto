package hydro.common

import com.google.inject.Inject
import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import play.api.i18n.Lang
import play.api.i18n.Langs
import play.api.i18n.MessagesApi

trait PlayI18n extends I18n {

  /** Returns a map that maps key to the message with placeholders. */
  def allI18nMessages: Map[String, String]
}

object PlayI18n {
  final class Impl @Inject() (implicit val messagesApi: MessagesApi, langs: Langs) extends PlayI18n {

    private val defaultLang: Lang = {
      require(langs.availables.size == 1, "Only a single language is supported at a time.")
      getOnlyElement(langs.availables)
    }

    // ****************** Implementation of PlayI18n trait ****************** //
    override def apply(key: String, args: Any*): String = {
      messagesApi(key, args: _*)(defaultLang)
    }

    override val allI18nMessages: Map[String, String] = {
      // defaultLang is extended by "default" in case it didn't overwrite a message key.
      messagesApi.messages("default") ++ messagesApi.messages(defaultLang.code)
    }
  }
}
