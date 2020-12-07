package hydro.common

/**
 * Wrapper class for any value. Different instances of `Unique` are never equal to each other.
 */
final class Unique[V] private (value: V) {
  def get: V = value
}

object Unique {
  def apply[V](value: V): Unique[V] = new Unique(value)
}
