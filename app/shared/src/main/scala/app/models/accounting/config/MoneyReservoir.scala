package app.models.accounting.config

import hydro.common.Require.requireNonNull
import app.common.money.Currency

case class MoneyReservoir(
    code: String,
    name: String,
    shorterName: String,
    owner: Account,
    hidden: Boolean,
    currencyCode: Option[String] = None,
    // If true, Facto will assume that the underlying asset fluctuates with the index (currency wih code
    // <index>). Whenever an balance check for that reservoir with tag is seen, Facto will attribute all
    // inflation since the last balance check to that moment in time.
    //
    // For example: I buy a house for 100k. 5 years later, I sell it for 140k. A balance check entry should be
    // made after adding +40k to the house value as appreciation at the time of sale, because this is the only
    // time there is reliable information on the value of the house. If inflation over these 5 years was 20%,
    // the generated inflation category will be zero for all months, except for the month of sale, where it
    // will be -20k. The sum of the appreciation and inflation will be 20k, which represents the appreciation
    // corrected for inflation.
    assumeThisFollowsInflationUntilNextBalanceCheck: Boolean,
) {
  requireNonNull(code, name, shorterName, owner, hidden, currencyCode)

  override def toString = s"MoneyReservoir($code)"

  lazy val currency: Currency = currencyCode match {
    case Some(code) => Currency.of(code)
    case None       => Currency.default
  }

  def isNullReservoir: Boolean = this.code.isEmpty
}

object MoneyReservoir {
  val NullMoneyReservoir: MoneyReservoir = MoneyReservoir(
    code = "",
    name = "N/A",
    shorterName = "N/A",
    owner = Account(
      code = "UNKNOWN",
      longName = "UNKNOWN",
      shorterName = "UNKNOWN",
      veryShortName = "UNKNOWN",
      defaultElectronicReservoirCode = "",
    ),
    hidden = true,
    assumeThisFollowsInflationUntilNextBalanceCheck = false,
  )
}
