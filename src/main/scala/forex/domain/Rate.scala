package forex.domain

import cats.Show
import cats.implicits.showInterpolator

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  implicit val showPair: Show[Pair] = Show.show(p => s"${p.from} 🠖 ${p.to}")

  implicit val showRate: Show[Rate] = Show.show { r =>
    show"Rate(${r.pair}, ${r.price.value}, ${r.timestamp})"
  }
}
