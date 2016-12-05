package common

/**
  * Replaces some Guava-provided functionality that is no longer usable with scala.js.
  */
object GuavaReplacement {

  object Iterables {
    def getOnlyElement[T](traversable: Traversable[T]): T = {
      val list = traversable.toList
      require(list.size == 1, s"Given traversable can only have one element but is $list")
      list.head
    }
  }

  object DoubleMath {
    def roundToLong(x: Double): Long = {
      StrictMath.rint(x).toLong
    }
  }
}
