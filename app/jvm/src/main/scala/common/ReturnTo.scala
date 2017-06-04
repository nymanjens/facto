package common

/**
  * Represents the URL to return to after the current flow is finished.
  *
  * Note: don't pass given URL as route parameter directly because Play Framework will escape
  * the slashes, which is annoying when setting up Facto with Apache.
  */
trait ReturnTo extends GetParameter {

  /**
    * The URL to return to.
    *
    * Note: don't pass this URL as route parameter directly because Play Framework will escape
    * the slashes, which is annoying when setting up Facto with Apache.
    **/
  def url: String
}

object ReturnTo {

  def apply(returnUrl: String): ReturnTo =
    new GetParameter.Impl(key = "returnTo", value = returnUrl) with ReturnTo {
      override def url = returnUrl
    }
}
