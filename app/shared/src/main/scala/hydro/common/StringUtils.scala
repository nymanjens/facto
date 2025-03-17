package hydro.common

import java.nio.charset.Charset
import java.util.stream.Collectors
import scala.collection.JavaConverters._

object StringUtils {

  private final val INVISIBLE_CHARACTER_TYPES: Set[Int] =
    Set(
      Character.CONTROL,
      Character.FORMAT,
      Character.PRIVATE_USE,
      Character.SURROGATE,
      Character.UNASSIGNED,
      Character.SPACE_SEPARATOR,
      Character.LINE_SEPARATOR,
      Character.PARAGRAPH_SEPARATOR,
    )

  def sanitizeSpecializedCharacters(
      string: String,
      stripNewlines: Boolean,
      substituteNonLatin1: Boolean,
  ): String = {
    val result: Seq[String] = for (char <- string) yield {
      char match {
        // Special case: Special quotes to normal quotes
        case '‘' => "'"
        case '‚' => "'"
        case '’' => "'"
        case '“' => "\""
        case '„' => "\""
        case '”' => "\""

        // Supported by latin1, but with a different code
        case '€' => "€"
        case '‰' => "‰"

        // Invisible characters: Only " " and "\n" are allowed as whitespace
        case ' '                                                              => " "
        case '\n'                                                             => if (stripNewlines) "" else "\n"
        case 0xa0                                                             => " " // Non-breaking space
        case '\t'                                                             => "  "
        case _ if INVISIBLE_CHARACTER_TYPES.contains(Character.getType(char)) => ""

        // Substitute non latin1
        case _ =>
          if (substituteNonLatin1) {
            // Strip all unicode characters that are not supported by Latin1
            val charset = Charset.forName("ISO-8859-1")
            new String(char.toString.getBytes(charset), charset)
          } else {
            char.toString
          }
      }
    }

    result.mkString
  }

  def containsSpecialCharacters(
      string: String,
      newlinesAreSpecial: Boolean,
      nonLatin1AreSpecial: Boolean,
  ): Boolean = {
    val sanitized = sanitizeSpecializedCharacters(
      string,
      stripNewlines = newlinesAreSpecial,
      substituteNonLatin1 = nonLatin1AreSpecial,
    )
    sanitized != string
  }

  def toStringWithSpecializedCharactersEscaped(
      string: String,
      escapeNewlines: Boolean,
      escapeNonLatin1: Boolean,
  ): String = {
    string.map {
      case c
          if containsSpecialCharacters(
            c.toString,
            newlinesAreSpecial = escapeNewlines,
            nonLatin1AreSpecial = escapeNonLatin1,
          ) =>
        f"\\u$c%04X"
      case c => c.toString
    }.mkString
  }
}
