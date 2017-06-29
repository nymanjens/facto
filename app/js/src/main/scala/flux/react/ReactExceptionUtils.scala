package flux.react

import japgolly.scalajs.react.CtorType
import japgolly.scalajs.react.component.Scala.{MountedImpure, MutableRef}
import scala.language.higherKinds

object ReactExceptionUtils {

  final class BrokenReferenceException extends Exception

  def valueOrThrow[P, S, B, CT[-p, +u] <: CtorType[p, u]](
      mutableRef: MutableRef[P, S, B, CT]): MountedImpure[P, S, B] = {
    val result = mutableRef.value
    if (result == null) {
      throw new BrokenReferenceException
    }
    result
  }
}
