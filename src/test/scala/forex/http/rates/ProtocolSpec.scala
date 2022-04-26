package forex.http.rates

import org.scalatest._
import flatspec._
import forex.domain.{ Currency, Price, Timestamp }
import io.circe.Json
import io.circe.syntax.EncoderOps
import matchers._
import io.circe.parser._

import java.time.OffsetDateTime

class ProtocolSpec extends AnyFlatSpec with should.Matchers {

  import forex.http.rates.Protocol._

  "A Protocol" should "serialize OneFrameApiRecord properly" in {
    val record = OneFrameApiRecord(
      from = Currency.CAD,
      to = Currency.CHF,
      price = Price(BigDecimal(1.01)),
      timeStamp = Timestamp(OffsetDateTime.parse("2022-04-20T19:49:41.546782+03:00"))
    )

    val json =
      """{
         |"from": "CAD",
         |"to": "CHF",
         |"price": 1.01,
         |"time_stamp": "2022-04-20T19:49:41.546782+03:00"
         |}
         |""".stripMargin

    val jsonObj = parse(json).getOrElse(Json.Null)

    record.asJson shouldBe jsonObj
  }
}
