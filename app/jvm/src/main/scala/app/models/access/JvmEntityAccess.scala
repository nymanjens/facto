package app.models.access

import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.money.ExchangeRateMeasurement
import app.models.slick.SlickEntityTableDefs
import app.models.user.User
import com.google.inject._
import hydro.common.time.Clock
import hydro.models.access.InMemoryEntityDatabase
import hydro.models.access.JvmEntityAccessBase
import hydro.models.modification.EntityType
import hydro.models.slick.SlickEntityTableDef

final class JvmEntityAccess @Inject()(implicit clock: Clock)
    extends JvmEntityAccessBase
    with AppEntityAccess {

  override def newQuerySyncForUser() = newQuerySync[User]()

  override protected def getEntityTableDef(
      entityType: EntityType.any): SlickEntityTableDef[entityType.get] = {
    val tableDef = entityType match {
      case User.Type                    => SlickEntityTableDefs.UserDef
      case Transaction.Type             => SlickEntityTableDefs.TransactionDef
      case TransactionGroup.Type        => SlickEntityTableDefs.TransactionGroupDef
      case BalanceCheck.Type            => SlickEntityTableDefs.BalanceCheckDef
      case ExchangeRateMeasurement.Type => SlickEntityTableDefs.ExchangeRateMeasurementDef
    }
    tableDef.asInstanceOf[SlickEntityTableDef[entityType.get]]
  }

  override protected def sortings =
    InMemoryEntityDatabase.Sortings.create
      .withSorting(AppDbQuerySorting.Transaction.deterministicallyByTransactionDate)
      .withSorting(AppDbQuerySorting.Transaction.deterministicallyByConsumedDate)
      .withSorting(AppDbQuerySorting.Transaction.deterministicallyByCreateDate)
      .withSorting(AppDbQuerySorting.BalanceCheck.deterministicallyByCheckDate)
}
