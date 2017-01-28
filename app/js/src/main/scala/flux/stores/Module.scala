package flux.stores

import models.accounting._
import models.accounting.money._

object Module {

  import com.softwaremill.macwire._
  import common.time.Module._
  import flux.Module._
  import models.access.Module._
  import models.Module._

  implicit lazy val lastNEntriesStoreFactory = wire[LastNEntriesStoreFactory]
  implicit lazy val transactionAndGroupStore = wire[TransactionAndGroupStore]
}
