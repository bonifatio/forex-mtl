package forex.services.rates.interpreters

import cats.effect.{BracketThrow, Resource}
import cats.implicits._
import forex.domain.{Currency, Rate}
import forex.services.rates.Algebra
import forex.services.rates.client.RatesClient
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.errors._

class OneFrameReal[F[_]: BracketThrow](clientFactory: => Resource[F, RatesClient[F]]) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val currencyFrom = pair.from
    val fromToPairs  = Currency.values.collect { case c if c != currencyFrom => Rate.Pair(currencyFrom, c) }

    clientFactory.use { client =>
      for {
        rates <- client.getRates(fromToPairs)
      } yield {

        rates.find(_.to == pair.to) match {
          case Some(rate) =>
            Rate(pair, rate.price, rate.timeStamp).asRight

          case None =>
            OneFrameLookupFailed(s"No exchange rate found for pair $pair").asLeft
        }
      }
    }
  }

}
