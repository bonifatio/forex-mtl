package forex.services.rates.interpreters

import cats.effect.{ IO, Resource, Timer }
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.generators.{ pairGen, priceGen, timestampGen }
import forex.http.rates.Protocol.OneFrameApiRecord
import forex.services.rates.client.RatesClient
import org.typelevel.log4cats.noop.NoOpLogger
import scalacache.caffeine.CaffeineCache
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import java.time.temporal.ChronoUnit
import scala.concurrent.duration._

object OneFrameRealSuite extends SimpleIOSuite with Checkers {
  override def maxParallelism: Int = 1

  implicit val logger = NoOpLogger[IO]
  val ratesCache      = CaffeineCache[List[OneFrameApiRecord]]
  implicit val mode   = scalacache.CatsEffect.modes.async

  def successfulClient(price: Price, timestamp: Timestamp): RatesClient[IO] =
    new RatesClient[IO] {
      override def getRates(pairs: List[Rate.Pair]): IO[List[OneFrameApiRecord]] =
        IO.pure {
          pairs.map { p =>
            OneFrameApiRecord(
              from = p.from,
              to = p.to,
              price = price,
              timeStamp = timestamp
            )
          }
        }
    }

  def stubClient(): RatesClient[IO] = (_: List[Rate.Pair]) => IO.never

  test("getting exchange rate from API") {
    val gen =
      for {
        pair <- pairGen
        price <- priceGen
        timestamp <- timestampGen
      } yield {
        (pair, price, timestamp)
      }

    forall(gen) {
      case (pair, price, timestamp) =>
        val oneFrame = new OneFrameReal[IO](
          cache = ratesCache,
          ratesTTL = 5.minutes,
          clientFactory = Resource.pure(successfulClient(price, timestamp))
        )

        for {
          resultRate <- ratesCache.doRemoveAll() *> oneFrame.get(pair)
        } yield {
          expect.same(resultRate, Right(Rate(pair, price, timestamp)))
        }
    }
  }

  test("getting exchange rate from the cache so that API is not hit") {
    val gen =
      for {
        pair <- pairGen
        price <- priceGen
        timestamp <- timestampGen
      } yield {
        (pair, price, timestamp)
      }

    forall(gen) {
      case (pair, price, timestamp) =>
        val oneFrame = new OneFrameReal[IO](
          cache = ratesCache,
          ratesTTL = 5.minutes,
          clientFactory = Resource.pure(stubClient())
        )

        val singleRate = OneFrameApiRecord(
          from = pair.from,
          to = pair.to,
          price = price,
          timeStamp = timestamp
        )

        for {
          resultRate <- ratesCache.put(pair.from.toString)(List(singleRate), ttl = None) *> oneFrame.get(pair)
        } yield {
          expect.same(resultRate, Right(Rate(pair, price, timestamp)))
        }
    }
  }

  test("getting exchange rate from the cache then from API") {

    val ratesTTL         = 10.milliseconds
    val pair             = Rate.Pair(Currency.USD, Currency.JPY)
    val cachedPrice      = Price(BigDecimal(3.45d))
    val updatedPrice     = Price(BigDecimal(4.56d))
    val cachedTimestamp  = Timestamp.now
    val updatedTimestamp = Timestamp(cachedTimestamp.value.plus(20, ChronoUnit.MILLIS))

    val oneFrame = new OneFrameReal[IO](
      cache = ratesCache,
      ratesTTL = ratesTTL,
      clientFactory = Resource.pure(successfulClient(updatedPrice, updatedTimestamp))
    )

    val cachedRate = OneFrameApiRecord(
      from = pair.from,
      to = pair.to,
      price = cachedPrice,
      timeStamp = cachedTimestamp
    )

    for {
      resultFromCache <- ratesCache.put("USD")(List(cachedRate), ttl = Some(ratesTTL)) *> oneFrame.get(pair)
      resultFromAPI <- Timer[IO].sleep(20.milliseconds) *> oneFrame.get(pair)
    } yield {
      expect.same(resultFromCache, Right(Rate(pair, cachedPrice, cachedTimestamp))) &&
      expect.same(resultFromAPI, Right(Rate(pair, updatedPrice, updatedTimestamp)))
    }
  }

}
