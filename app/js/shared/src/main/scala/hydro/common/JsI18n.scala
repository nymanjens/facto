package hydro.common

/**
  * @param i18nMessages Maps key to the message with placeholders.
  */
private[common] final class JsI18n(i18nMessages: Map[String, String]) extends I18n {

  // ****************** Implementation of I18n trait ****************** //
  override def apply(key: String, args: Any*): String = {
    val messageWithPlaceholders = i18nMessages.get(key).getOrElse(key)
    messageFormat(messageWithPlaceholders, args: _*)
  }

  // ****************** Helper methods ****************** //
  /**
    * Formats given message to include arguments at the placeholders.
    *
    * @param messageWithPlaceholders e.g. "Debt of {0} to {1}".
    * @param args                    Arguments to insert into message.
    */
  private def messageFormat(messageWithPlaceholders: String, args: Any*): String = {
    var result = messageWithPlaceholders
    for ((arg, i) <- args.zipWithIndex) {
      result = result.replaceAllLiterally(s"{$i}", arg.toString)
    }
    result
  }
}
