package app.models

import app.common.money.CurrencyValueManager
import app.models.access.AppEntityAccess
import app.models.access.JvmEntityAccess
import app.models.money.JvmCurrencyValueManager
import com.google.inject.AbstractModule
import hydro.models.access.EntityAccess

final class ModelsModule extends AbstractModule {
  override def configure() = {
    bind(classOf[AppEntityAccess]).to(classOf[JvmEntityAccess])
    bind(classOf[EntityAccess]).to(classOf[JvmEntityAccess])
    bind(classOf[JvmEntityAccess]).asEagerSingleton

    bind(classOf[CurrencyValueManager]).to(classOf[JvmCurrencyValueManager])
    bind(classOf[JvmCurrencyValueManager]).asEagerSingleton
  }
}
