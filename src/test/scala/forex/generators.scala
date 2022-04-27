package forex

import forex.domain.{ Currency, Price, Rate, Timestamp }
import org.scalacheck.Gen

import java.time.{ Instant, OffsetDateTime, ZoneId }

object generators {
  val priceGen: Gen[Price] =
    Gen.posNum[BigDecimal].map(Price.apply)

  val currencyGen: Gen[Currency] =
    Gen.oneOf(Currency.values)

  val timestampGen: Gen[Timestamp] =
    Gen
      .choose[Instant](Instant.MIN, Instant.MAX)
      .map(t => Timestamp(OffsetDateTime.ofInstant(t, ZoneId.of("UTC"))))

  val pairGen: Gen[Rate.Pair] =
    for {
      from <- currencyGen
      to <- currencyGen.retryUntil(_ != from)
    } yield Rate.Pair(from, to)
}
