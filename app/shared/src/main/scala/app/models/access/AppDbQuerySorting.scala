package app.models.access

import app.models.access.DbQuery.Sorting

import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction

object AppDbQuerySorting {

  object Transaction {
    val deterministicallyByTransactionDate: Sorting[Transaction] = DbQuery.Sorting
      .ascBy(ModelFields.Transaction.transactionDate)
      .thenAscBy(ModelFields.Transaction.createdDate)
      .thenAscBy(ModelFields.id)
    val deterministicallyByConsumedDate: Sorting[Transaction] = DbQuery.Sorting
      .ascBy(ModelFields.Transaction.consumedDate)
      .thenAscBy(ModelFields.Transaction.createdDate)
      .thenAscBy(ModelFields.id)
    val deterministicallyByCreateDate: Sorting[Transaction] = DbQuery.Sorting
      .ascBy(ModelFields.Transaction.createdDate)
      .thenAscBy(ModelFields.id)
  }
  object BalanceCheck {
    val deterministicallyByCheckDate: Sorting[BalanceCheck] = DbQuery.Sorting
      .ascBy(ModelFields.BalanceCheck.checkDate)
      .thenAscBy(ModelFields.BalanceCheck.createdDate)
      .thenAscBy(ModelFields.id)
  }
}
