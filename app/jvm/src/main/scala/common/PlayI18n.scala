package common

import play.api.i18n.{MessagesApi, Langs, Lang}
import com.google.inject.Inject
import common.GuavaReplacement.Iterables.getOnlyElement

private[common] final class PlayI18n @Inject()(implicit val messagesApi: MessagesApi,
                               langs: Langs) extends I18n {

  val defaultLang: Lang = {
    require(langs.availables.size == 1, "Only a single language is supported at a time.")
    getOnlyElement(langs.availables)
  }

  override def apply(key: String, args: Any*): String = {
    messagesApi(key, args)(defaultLang)
  }
}
