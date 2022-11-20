package app.flux.stores

import app.flux.stores.InMemoryUserConfigFactory.InMemoryUserConfig
import hydro.common.Listenable.WritableListenable
import hydro.flux.stores.StateStore

final class InMemoryUserConfigFactory extends StateStore[InMemoryUserConfig] {

  // **************** Private fields **************** //
  private var inMemoryState: WritableListenable[InMemoryUserConfig] =
    WritableListenable[InMemoryUserConfig](InMemoryUserConfig())
  inMemoryState.registerListener(newValue => invokeStateUpdateListeners())

  // **************** Public API ****************//
  override def state: InMemoryUserConfig = inMemoryState.get

  def mutateState(mutation: InMemoryUserConfig => InMemoryUserConfig): Unit = {
    inMemoryState.set(mutation(inMemoryState.get))
  }
}
object InMemoryUserConfigFactory {
  case class InMemoryUserConfig(correctForInflation: Boolean = false)
}
