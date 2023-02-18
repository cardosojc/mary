package io.cardosojcs.mary

import cats.effect.IO
import com.comcast.ip4s._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

object MaryServer {

  def run: IO[Nothing] = {
    for {
      _ <- EmberClientBuilder.default[IO].build
      shotAlg = DiabetesShot.impl

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = MaryRoutes.shotRoutes(shotAlg).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      _ <- 
        EmberServerBuilder.default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8081")
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever
}
