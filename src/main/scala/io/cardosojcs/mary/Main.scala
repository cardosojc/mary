package io.cardosojcs.mary

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run = MaryServer.run[IO]
}
