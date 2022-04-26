package forex.services.rates

import cats.effect.{ BracketThrow, Resource }
import cats.Applicative
import forex.services.rates.client.RatesClient
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] =
    new OneFrameDummy[F]()

  def live[F[_]: BracketThrow](clientFactory: => Resource[F, RatesClient[F]]): Algebra[F] =
    new OneFrameReal[F](clientFactory)
}
