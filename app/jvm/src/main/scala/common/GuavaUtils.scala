package common

import com.google.common.base.Function

object GuavaUtils {

  def asGuava[F, T](function: F => T): Function[F, T] = new Function[F, T]() {
    override def apply(input: F): T = function(input)
  }
}
