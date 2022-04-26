package forex.services.rates.client

import cats.effect.BracketThrow
import forex.domain.Rate
import forex.http.rates.Protocol.OneFrameApiRecord
import org.http4s.{ Header, MediaType, Uri }
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import cats.implicits._
import forex.config.OneFrameApiConfig
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.http4s.headers.Accept
import org.http4s._
import org.http4s.circe._

trait RatesClient[F[_]] {
  def getRates(pairs: List[Rate.Pair]): F[List[OneFrameApiRecord]]
}

object RatesClient {
  def make[F[_]: BracketThrow: JsonDecoder](
      client: Client[F],
      config: OneFrameApiConfig
  ): RatesClient[F] =
    new RatesClient[F] with Http4sClientDsl[F] {
      val baseUri = s"${config.http.scheme}://${config.http.host}:${config.http.port}"

      def getRates(pairs: List[Rate.Pair]): F[List[OneFrameApiRecord]] = {
        import forex.http.rates.Protocol._

        val pairsFormatted =
          Map("pair" -> pairs.map(pair => s"${pair.from}${pair.to}"))

        Uri
          .fromString(baseUri + "/rates")
          .liftTo[F]
          .flatMap { uri =>
            val request = Request[F](Method.GET)
              .withUri(uri.withMultiValueQueryParams(pairsFormatted))
              .withHeaders(Header("token", config.token), Accept(MediaType.application.json))

            client.run(request).use { resp =>
              resp.status match {
                case Status.Ok =>
                  resp.asJsonDecode[List[OneFrameApiRecord]]
                case status =>
                  val msg = Option(status.reason).getOrElse("Unknown error during call of OneFrame service")
                  OneFrameLookupFailed(msg).raiseError[F, List[OneFrameApiRecord]]
              }
            }
          }
      }
    }
}
