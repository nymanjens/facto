package app.flux.stores

import app.flux.stores.InMemoryUserConfigStore.InMemoryUserConfig
import hydro.common.Listenable.WritableListenable
import hydro.flux.stores.StateStore
import app.models.accounting.config.Config

final class InMemoryUserConfigStore(implicit
    accountingConfig: Config
) extends StateStore[InMemoryUserConfig] {

  // **************** Private fields **************** //
  private var inMemoryState: WritableListenable[InMemoryUserConfig] =
    WritableListenable[InMemoryUserConfig](
      InMemoryUserConfig(correctForInflation = accountingConfig.constants.supportInflationCorrections)
    )
  inMemoryState.registerListener(newValue => invokeStateUpdateListeners())

  // **************** Public API ****************//
  override def state: InMemoryUserConfig = inMemoryState.get

  def mutateState(mutation: InMemoryUserConfig => InMemoryUserConfig): Unit = {
    inMemoryState.set(mutation(inMemoryState.get))
  }
}
object InMemoryUserConfigStore {
  case class InMemoryUserConfig(correctForInflation: Boolean)
}
