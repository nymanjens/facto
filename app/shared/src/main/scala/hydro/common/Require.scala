package hydro.common

object Require {

  def requireNonNull(objects: Any*): Unit = {
    for ((obj, index) <- objects.zipWithIndex) {
      require(obj != null, s"Reference at index $index is null")
    }
  }
}
