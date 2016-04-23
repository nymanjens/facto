package models.accounting.config

import common.Require.requireNonNullFields

import collection.immutable.Seq

case class Category(code: String, name: String, helpText: String = "") {
  requireNonNullFields(this)
  
  def accounts: Seq[Account] = Config.accounts.values.filter(_.categories.contains(this)).toList

  override def toString = s"Category($code)"
}
