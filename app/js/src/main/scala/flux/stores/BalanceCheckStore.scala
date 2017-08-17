package flux.stores

import common.time.Clock
import flux.action.Action.{AddBalanceCheck, RemoveBalanceCheck, UpdateBalanceCheck}
import flux.action.Dispatcher
import models.EntityAccess
import models.access.RemoteDatabaseProxy
import models.manager.EntityModification

final class BalanceCheckStore(implicit database: RemoteDatabaseProxy,
                              dispatcher: Dispatcher) {
  dispatcher.registerPartialAsync {
    case AddBalanceCheck(balanceCheckWithoutId) =>
      database.persistModifications(EntityModification.createAddWithRandomId(balanceCheckWithoutId))

    case UpdateBalanceCheck(balanceCheckWithId) =>
      val bcDeletion = EntityModification.createDelete(balanceCheckWithId)
      val bcAddition = EntityModification.createAddWithRandomId(balanceCheckWithId.copy(idOption = None))
      database.persistModifications(bcDeletion, bcAddition)

    case RemoveBalanceCheck(balanceCheckWithId) =>
      database.persistModifications(EntityModification.createDelete(balanceCheckWithId))
  }
}
