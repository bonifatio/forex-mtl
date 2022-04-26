package forex.services.rates

import cats.effect.{ Async, Resource }
import cats.Applicative
import forex.http.rates.Protocol.OneFrameApiRecord
import forex.services.rates.client.RatesClient
import interpreters._
import org.typelevel.log4cats.Logger
import scalacache.Cache

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] =
    new OneFrameDummy[F]()

  def live[F[_]: Async: Logger](
      cache: Cache[List[OneFrameApiRecord]],
      ratesTTL: FiniteDuration,
      clientFactory: => Resource[F, RatesClient[F]]
  ): Algebra[F] =
    new OneFrameReal[F](cache, ratesTTL, clientFactory)
}
