package forex.http.rates

import cats.effect.IO
import forex.domain.{ Price, Rate, Timestamp }
import forex.http.rates.Protocol.GetApiResponse
import forex.generators.{ pairGen, priceGen, timestampGen }
import weaver.scalacheck.Checkers
import forex.HttpSuite
import forex.programs.RatesProgram
import forex.programs.rates.{ errors, Protocol }
import org.http4s._
import org.http4s.syntax.literals._

object RatesHttpRoutesSuite extends HttpSuite with Checkers {
  def successfulRateProgram(pair: Rate.Pair, price: Price, timestamp: Timestamp): TestRatesProgram =
    new TestRatesProgram {
      override def get(request: Protocol.GetRatesRequest): IO[Either[errors.Error, Rate]] =
        IO.pure(Right(Rate(pair, price, timestamp)))
    }

  def failingRateProgram(): TestRatesProgram =
    new TestRatesProgram {
      override def get(request: Protocol.GetRatesRequest): IO[Either[errors.Error, Rate]] =
        IO.raiseError(DummyError)
    }

  test("GET rate succeeds") {
    val gen = for {
      pair <- pairGen
      price <- priceGen
      timestamp <- timestampGen
    } yield (pair, price, timestamp)

    forall(gen) {
      case (pair, price, timestamp) =>
        val uri     = uri"/rates".withQueryParam("from", pair.from.toString).withQueryParam("to", pair.to.toString)
        val request = Request[IO](Method.GET).withUri(uri)

        val routes = new RatesHttpRoutes[IO](successfulRateProgram(pair, price, timestamp)).routes

        val expectedResponse = GetApiResponse(
          from = pair.from,
          to = pair.to,
          price = price,
          timestamp = timestamp
        )

        expectHttpBodyAndStatus(routes, request)(expectedResponse, Status.Ok)
    }
  }

  test("GET items fails") {

    forall(pairGen) {
      case pair =>
        val uri     = uri"/rates".withQueryParam("from", pair.from.toString).withQueryParam("to", pair.to.toString)
        val request = Request[IO](Method.GET).withUri(uri)

        val routes = new RatesHttpRoutes[IO](failingRateProgram()).routes
        expectHttpFailure(routes, request)
    }
  }

  protected class TestRatesProgram extends RatesProgram[IO] {
    override def get(request: Protocol.GetRatesRequest): IO[Either[errors.Error, Rate]] = IO.never
  }

}
