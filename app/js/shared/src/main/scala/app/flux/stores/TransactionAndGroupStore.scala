package app.flux.stores

import app.flux.action.AppActions.AddTransactionGroup
import app.flux.action.AppActions.RefactorAction
import app.flux.action.AppActions.RemoveTransactionGroup
import app.flux.action.AppActions.UpdateTransactionGroup
import app.models.access.AppDbQuerySorting
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.accounting._
import app.models.accounting.config.Config
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.models.access.DbQueryImplicits._
import hydro.models.modification.EntityModification
import org.scalajs.dom.console

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[stores] final class TransactionAndGroupStore(implicit
    entityAccess: AppJsEntityAccess,
    clock: Clock,
    dispatcher: Dispatcher,
    accountingConfig: Config,
) {
  dispatcher.registerPartialAsync {
    // **************** Transaction[Group]-related actions **************** //
    case AddTransactionGroup(transactionsWithoutIdProvider) =>
      async {
        val groupAddition =
          EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
        val group = groupAddition.entity
        val transactionsWithoutId = transactionsWithoutIdProvider(group)
        val transactionAdditions =
          for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
            EntityModification.createAddWithId(id, transactionWithoutId)
          }
        await(entityAccess.persistModifications(groupAddition +: transactionAdditions))
      }

    case UpdateTransactionGroup(group, transactionsWithoutId) =>
      async {
        val transactionDeletions = await(group.transactions) map (EntityModification.createRemove(_))
        val transactionAdditions =
          for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
            EntityModification.createAddWithId(id, transactionWithoutId)
          }
        await(entityAccess.persistModifications(transactionDeletions ++ transactionAdditions))
      }

    case RemoveTransactionGroup(group) =>
      async {
        val transactionDeletions = await(group.transactions) map (EntityModification.createRemove(_))
        val groupDeletion = EntityModification.createRemove(group)
        await(entityAccess.persistModifications(transactionDeletions :+ groupDeletion))
      }

    // **************** Refactor actions **************** //
    case refactorAction: RefactorAction =>
      applyRefactor(refactorAction)
  }

  private def zipWithIncrementingId[E](entities: Seq[E]): Seq[(E, Long)] = {
    val ids = {
      val start = EntityModification.generateRandomId()
      val end = start + entities.size
      start until end
    }
    entities zip ids
  }

  private def applyRefactor(refactorAction: RefactorAction): Future[Unit] = async {
    val affectedTransactions = refactorAction.affectedTransactions

    val modifications = await(
      Future.sequence(
        for {
          transactionGroupId <- affectedTransactions.map(_.transactionGroupId).distinct
        } yield {
          getRefactorModifications(
            transactionGroupId,
            refactorAction.updateToApply,
            shouldBeEdited = affectedTransactions.contains,
          )
        }
      )
    ).flatten

    entityAccess.persistModifications(modifications)

    console.log(
      s"""
         |#given transactions                  = ${refactorAction.transactions.size}
         |#transactions that were edited       = ${affectedTransactions.size}
         |
         |Transactions that were edited:
         |
         |${affectedTransactions.map(t => s"- ${t.description} (${t.category}, ${t.tags})\n").mkString("")}
         |""".stripMargin
    )
  }

  private def getRefactorModifications(
      transactionGroupId: Long,
      updateToApply: Transaction => Transaction,
      shouldBeEdited: Transaction => Boolean,
  ): Future[Seq[EntityModification]] = async {
    (for {
      (transaction, newId) <- zipWithIncrementingId(
        await(
          entityAccess
            .newQuery[Transaction]()
            .filter(ModelFields.Transaction.transactionGroupId === transactionGroupId)
            .sort(AppDbQuerySorting.Transaction.deterministicallyByCreateDate)
            .data()
        )
      )
    } yield Seq(
      EntityModification.createRemove(transaction),
      EntityModification.createAddWithId(
        newId,
        (
          if (shouldBeEdited(transaction)) updateToApply(transaction)
          else transaction
        ).copy(idOption = None),
      ),
    )).flatten
  }
}
