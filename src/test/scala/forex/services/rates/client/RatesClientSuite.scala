package forex.services.rates.client

import cats.effect.IO
import forex.config.{ HttpConfig, OneFrameApiConfig }
import forex.domain.{ Currency, Price, Rate, Timestamp }
import org.http4s.{ HttpApp, QueryParamDecoder }
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import forex.http.rates.Protocol.OneFrameApiRecord
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{ HttpRoutes, Response }
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.concurrent.duration._

object RatesClientSuite extends SimpleIOSuite with Checkers {

  val config = OneFrameApiConfig(
    http = HttpConfig(host = "localhost", scheme = "http", port = 8080, timeout = 40.seconds),
    token = "",
    ratesTimeToLive = 5.minutes
  )

  private implicit val pairQueryParam: QueryParamDecoder[Rate.Pair] =
    QueryParamDecoder[String].map { s =>
      val (from, to) = s.splitAt(3)
      Rate.Pair(Currency.fromString(from), Currency.fromString(to))
    }

  object PairQueryParam extends QueryParamDecoderMatcher[Rate.Pair]("pair")

  def routes(mkResponse: IO[Response[IO]]): HttpApp[IO] =
    HttpRoutes
      .of[IO] {
        case GET -> Root / "rates" :? PairQueryParam(_) => mkResponse
      }
      .orNotFound

  test("Response Ok (200)") {
    val responseItems = List(
      OneFrameApiRecord(Currency.USD, Currency.JPY, Price(1.23d), Timestamp.now),
      OneFrameApiRecord(Currency.JPY, Currency.USD, Price(0.93d), Timestamp.now)
    )

    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY), Rate.Pair(Currency.JPY, Currency.USD))

    val client = Client.fromHttpApp(routes(Ok(responseItems)))

    RatesClient
      .make[IO](client, config)
      .getRates(pairs)
      .map(expect.same(responseItems, _))
  }

  test("Internal Server Error response (500)") {

    val client = Client.fromHttpApp(routes(InternalServerError()))
    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY), Rate.Pair(Currency.JPY, Currency.USD))

    RatesClient
      .make[IO](client, config)
      .getRates(pairs)
      .attempt
      .map {
        case Left(e) =>
          expect.same(OneFrameLookupFailed("Internal Server Error"), e)

        case Right(_) =>
          failure("expected API error")
      }
  }
}
