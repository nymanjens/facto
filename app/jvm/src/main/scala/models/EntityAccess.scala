package models

import com.google.inject._
import collection.immutable.Seq
import models.manager.SlickEntityManager
import models.accounting._
import models.accounting.money.ExchangeRateMeasurements

class EntityAccess @Inject()(implicit val userManager: User.Manager,
                             val balanceCheckManager: BalanceCheck.Manager,
                             val tagEntityManager: TagEntity.Manager)

