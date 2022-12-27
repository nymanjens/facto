package hydro.flux.stores

import scala.collection.immutable.Seq

case class FixedStateStore[State](fixedState: State) extends StateStore[State] {
  override final def state: State = fixedState
}
