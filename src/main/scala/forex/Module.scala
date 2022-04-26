package forex

import cats.effect.{ Concurrent, Resource, Timer }
import forex.config.ApplicationConfig
import forex.http.rates.Protocol.OneFrameApiRecord
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import forex.services.rates.client.RatesClient
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import org.typelevel.log4cats.Logger
import scalacache.Cache

class Module[F[_]: Concurrent: Timer: Logger](config: ApplicationConfig,
                                              cache: Cache[List[OneFrameApiRecord]],
                                              clientFactory: => Resource[F, RatesClient[F]]) {

  private val ratesService: RatesService[F] =
    RatesServices.live[F](cache, config.oneFrameApi.ratesTimeToLive, clientFactory)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
