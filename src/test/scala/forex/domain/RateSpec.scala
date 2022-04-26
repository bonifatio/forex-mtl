package forex.domain

import cats.implicits.toShow
import org.scalatest._
import flatspec._
import matchers._

import java.time.OffsetDateTime

class RateSpec extends AnyFlatSpec with should.Matchers {

  "A Rate" should "be shown properly" in {
    import Rate._

    val rate = Rate(
      Pair(Currency.CAD, Currency.CHF),
      Price(BigDecimal(1.23)),
      Timestamp(OffsetDateTime.parse("2022-04-20T19:49:41.546782+03:00"))
    )

    rate.show shouldBe """Rate(CAD 🠖 CHF, 1.23, 20.04.2022 19:49:41.546+0300)"""
  }
}
