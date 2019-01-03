package app.models.accounting.config

import hydro.common.Require.requireNonNull

import scala.collection.immutable.Seq

case class Category(code: String, name: String, helpText: String = "") {
  requireNonNull(code, name, helpText)

  def accounts(implicit accountingConfig: Config): Seq[Account] =
    accountingConfig.accounts.values.filter(_.categories.contains(this)).toList

  override def toString = s"Category($code)"
}
