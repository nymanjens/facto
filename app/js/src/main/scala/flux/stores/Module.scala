package flux.stores

import models.access.RemoteDatabaseProxy
import models.accounting._
import models.accounting.money._

final class Module(implicit remoteDatabaseProxy: RemoteDatabaseProxy) {

  import com.softwaremill.macwire._
  import common.time.Module._
  import flux.action.Module._

  val modelsModule = new models.Module
  import modelsModule._

  implicit lazy val allEntriesStoreFactory = wire[AllEntriesStoreFactory]
  implicit lazy val transactionAndGroupStore = wire[TransactionAndGroupStore]
}
