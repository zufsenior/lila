package lila.plan

import org.joda.time.DateTime

case class PayPalIpnKey(value: String) extends AnyVal

case class CustomerId(value: String) extends AnyVal
case class ChargeId(value: String) extends AnyVal

case class Source(value: String) extends AnyVal

sealed abstract class Freq(val renew: Boolean)
object Freq {
  case object Monthly extends Freq(renew = true)
  case object Onetime extends Freq(renew = false)
}

case class Usd(value: BigDecimal) extends AnyVal with Ordered[Usd] {
  def compare(other: Usd) = value compare other.value
  def cents = Cents((value * 100).toInt)
  def toFloat = value.toFloat
  def toInt = value.toInt
  override def toString = s"$$$value"
}
object Usd {
  def apply(value: Double): Usd = Usd(BigDecimal(value))
  def apply(value: Int): Usd = Usd(BigDecimal(value))
}
case class Cents(value: Int) extends AnyVal with Ordered[Cents] {
  def compare(other: Cents) = value compare other.value
  def usd = Usd(value / 100d)
  override def toString = usd.toString
}

object Cents {
  val lifetime = Cents(25000)
}

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripePlan(id: String, name: String, amount: Cents) {
  def cents = amount
  def usd = cents.usd
}
object StripePlan {
  def make(cents: Cents, freq: Freq): StripePlan = freq match {
    case Freq.Monthly => StripePlan(
      id = s"monthly_${cents.value}",
      name = s"Monthly ${cents.usd}",
      amount = cents
    )
    case Freq.Onetime => StripePlan(
      id = s"onetime_${cents.value}",
      name = s"One-time ${cents.usd}",
      amount = cents
    )
  }

  val defaultAmounts = List(5, 10, 20, 50).map(Usd.apply).map(_.cents)
}

case class StripeSubscription(
    id: String,
    plan: StripePlan,
    customer: CustomerId,
    cancel_at_period_end: Boolean
) {

  def renew = !cancel_at_period_end
}

case class StripeCustomer(
    id: CustomerId,
    email: Option[String],
    subscriptions: StripeSubscriptions
) {

  def firstSubscription = subscriptions.data.headOption

  def plan = firstSubscription.map(_.plan)

  def renew = firstSubscription ?? (_.renew)
}

case class StripeCharge(id: ChargeId, amount: Cents, customer: CustomerId) {
  def lifetimeWorthy = amount >= Cents.lifetime
}

case class StripeInvoice(
    id: Option[String],
    amount_due: Int,
    date: Long,
    paid: Boolean
) {
  def cents = Cents(amount_due)
  def usd = cents.usd
  def dateTime = new DateTime(date * 1000)
}
