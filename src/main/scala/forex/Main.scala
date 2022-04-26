package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import forex.services.rates.client.RatesClient
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")

      module = new Module[F](
        config,
        BlazeClientBuilder[F](ec)
          .withRequestTimeout(config.oneFrameApi.http.timeout)
          .resource
          .map { client =>
            RatesClient.make[F](client, config.oneFrameApi)
          }
      )

      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
