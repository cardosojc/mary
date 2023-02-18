package io.cardosojcs.mary

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object MaryRoutes {

  def shotRoutes(H: DiabetesShot): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._
    HttpRoutes.of[IO] {
      case GET -> Root / "shot" =>
        for {
          greeting <- H.get
          resp <- Ok(greeting)
        } yield resp
    }
  }
}