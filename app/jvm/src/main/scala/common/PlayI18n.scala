package common

import play.api.i18n.{MessagesApi, Langs, Lang}
import com.google.inject.Inject
import common.GuavaReplacement.Iterables.getOnlyElement

final class PlayI18n @Inject()(implicit val messagesApi: MessagesApi,
                               langs: Langs) extends I18n {

  private val defaultLang: Lang = {
    require(langs.availables.size == 1, "Only a single language is supported at a time.")
    getOnlyElement(langs.availables)
  }

  // ****************** Implementation of I18n trait ****************** //
  override def apply(key: String, args: Any*): String = {
    messagesApi(key, args)(defaultLang)
  }

  // ****************** Additional API ****************** //
  /** Returns a map that maps key to the message with placeholders. */
  def allI18nMessages: Map[String, String] = {
    // defaultLang is extended by "default" in case it didn't overwrite a message key.
    messagesApi.messages("default") ++ messagesApi.messages(defaultLang.code)
  }
}
