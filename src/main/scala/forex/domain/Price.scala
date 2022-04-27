package forex.domain

import cats.Show

case class Price(value: BigDecimal) extends AnyVal

object Price {
  def apply(value: Integer): Price =
    Price(BigDecimal(value))

  implicit val showPair: Show[Price] = Show.show(_.value.toString)
}
