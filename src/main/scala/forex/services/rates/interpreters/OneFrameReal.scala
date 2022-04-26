package forex.services.rates.interpreters

import cats.effect.{ Async, Resource }
import cats.implicits._
import forex.domain.{ Currency, Rate }
import forex.services.rates.Algebra
import forex.services.rates.client.RatesClient
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.errors._
import org.typelevel.log4cats.Logger
import cats.implicits.showInterpolator
import forex.http.rates.Protocol.OneFrameApiRecord
import scalacache.Cache

import scala.concurrent.duration.FiniteDuration

class OneFrameReal[F[_]: Async: Logger](
    cache: Cache[List[OneFrameApiRecord]],
    ratesTTL: FiniteDuration,
    clientFactory: => Resource[F, RatesClient[F]]
) extends Algebra[F] {

  implicit val mode = scalacache.CatsEffect.modes.async

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val currencyFrom = pair.from

    cache
      .cachingF(currencyFrom.toString)(ratesTTL.some) {
        val fromToPairs = Currency.values.collect { case c if c != currencyFrom => Rate.Pair(currencyFrom, c) }

        clientFactory.use { client =>
          Logger[F].info(show"Making request for $pair") *> client.getRates(fromToPairs)
        }
      }
      .map { rates =>
        rates.find(_.to == pair.to) match {
          case Some(rate) =>
            Rate(pair, rate.price, rate.timeStamp).asRight

          case None =>
            OneFrameLookupFailed(s"No exchange rate found for pair $pair").asLeft
        }
      }
  }

}
