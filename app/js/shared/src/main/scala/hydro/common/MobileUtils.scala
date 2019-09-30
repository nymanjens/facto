package hydro.common

import hydro.common.JsLoggingUtils.logExceptions
import org.scalajs.dom

import scala.scalajs.js

object MobileUtils {

  lazy val isMobileOrTablet: Boolean = logExceptions {
    val navigator = dom.window.navigator
    val userAgent = maybeAsString(navigator.userAgent)
    val vendor = maybeAsString(navigator.asInstanceOf[js.Dynamic].vendor)
    val opera = maybeAsString(dom.window.asInstanceOf[js.Dynamic].opera)

    val stringToTest = userAgent orElse vendor orElse opera getOrElse ""

    stringContainsAnyOf(
      haystack = stringToTest,
      needles = Seq("android", "blackberry", "iphone", "ipad", "ipod", "opera mini", "iemobile", "wpdesktop"))
  }

  private def stringContainsAnyOf(haystack: String, needles: Seq[String]): Boolean = {
    needles.map(_.toLowerCase).exists(haystack.toLowerCase.contains)
  }

  private def maybeAsString(value: js.Any): Option[String] = {
    if (js.isUndefined(value)) {
      None
    } else {
      Some(value.asInstanceOf[String])
    }
  }
}
