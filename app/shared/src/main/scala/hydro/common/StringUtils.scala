package hydro.common

import java.nio.charset.Charset

object StringUtils {

  private val NEWLINE_INDICATOR = "%N)EWLI_(NE%*&"

  def cleanupForSpecializedCharacters(input: String, stripNewlines: Boolean): String = {
    var result = input

    if (stripNewlines) {
      result = result.replace("\n", "")
    } else {
      // Workaround for odd bug in p{Z} regex below that also seems to be filtering out newlines
      result = result.replace("\n", NEWLINE_INDICATOR)
    }
    result = result.replace("\t", "  ")

    // Strip any kind of whitespace or invisible separator.
    result = result.replaceAll("[\\p{Z}&&[^ \n]]", "")

    // Strip invisible control characters and unused code points
    result = result.replaceAll("\\p{C}", "")

    // Strip all unicode characters that are not supported by Latin1
    val charset = Charset.forName("ISO-8859-1")
    result = new String(result.getBytes(charset), charset)

    // Change back newline indicator to newline
    if (!stripNewlines) {
      result = result.replace(NEWLINE_INDICATOR, "\n")
    }

    result
  }
}
