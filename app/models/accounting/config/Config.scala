package models.accounting.config

import common.Require.requireNonNullFields

import collection.immutable.Seq

import play.Play.application

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess

import models.User

case class Config(accounts: Map[String, Account],
                  categories: Map[String, Category],
                  moneyReservoirs: Map[String, MoneyReservoir],
                  constants: Constants) {
  requireNonNullFields(this)
}

object Config {

  private val loadedConfig: Config = {
    // get configLocation
    val configLocation = application.configuration.getString("facto.accounting.configYamlFilePath")

    // get data
    val stringData = scala.io.Source.fromFile(configLocation).mkString

    // parse data
    val constr = new CustomClassLoaderConstructor(getClass.getClassLoader)
    val yaml = new Yaml(constr)
    yaml.setBeanAccess(BeanAccess.FIELD)
    val configData = yaml.load(stringData).asInstanceOf[Parsable.Config]

    // convert to parsed config
    configData.parse
  }

  /** Maps code to account */
  val accounts: Map[String, Account] = loadedConfig.accounts
  /** Maps code to category */
  val categories: Map[String, Category] = loadedConfig.categories
  /** Maps code to reservoir */
  val moneyReservoirs: Map[String, MoneyReservoir] = loadedConfig.moneyReservoirs

  val constants: Constants = loadedConfig.constants

  def accountOf(user: User): Option[Account] = accounts.values.filter(_.user == Some(user)).headOption

  def personallySortedAccounts(implicit user: User): Seq[Account] = {
    val myAccount = accountOf(user)
    val otherAccounts = for {
      acc <- accounts.values
      if !Set(Some(constants.commonAccount), myAccount).flatten.contains(acc)
    } yield acc
    Seq(List(constants.commonAccount), myAccount.toList, otherAccounts).flatten
  }
}
