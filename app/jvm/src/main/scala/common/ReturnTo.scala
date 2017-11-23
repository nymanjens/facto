package common

/**
  * Represents the URL to return to after the current flow is finished.
  *
  * Note: don't pass given URL as route parameter directly because Play Framework will escape
  * the slashes, which is annoying when setting up Facto with Apache.
  */
trait ReturnTo {}

object ReturnTo {

  def apply(returnUrl: String): ReturnTo = new ReturnTo() {}
}
