package controllers.helpers.accounting

import models.accounting.config.Account

case class AccountPair(account1: Account, account2: Account) {
  val toSet: Set[Account] = Set(account1, account2)
}