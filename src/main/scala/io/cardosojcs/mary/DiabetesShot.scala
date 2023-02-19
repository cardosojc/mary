package io.cardosojcs.mary

import cats.effect.IO
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

trait DiabetesShot {
  def get: IO[DiabetesShot.Shot]
}

object DiabetesShot {

  implicit val shotDecoder: Decoder[Shot] = deriveDecoder[Shot]

  implicit def shotEntityDecoder: EntityDecoder[IO, Shot] =
    jsonOf

  implicit val shotEncoder: Encoder[Shot] = deriveEncoder[Shot]

  implicit def shotEntityEncoder: EntityEncoder[IO, Shot] =
    jsonEncoderOf

  final case class Shot(place: String) extends AnyVal

  def impl: DiabetesShot = new DiabetesShot {
    def get: IO[DiabetesShot.Shot] = {
      Redis[IO].utf8(sys.env.getOrElse("REDIS_URL", "redis://localhost")).use { redis =>
        for {
          x <- redis.get("lastshot")
          next <- calculateNextShot(x)
          _ <- redis.set("lastshot", next.place)
        } yield next
      }
    }
  }

  private def calculateNextShot(value: Option[String]) = {
    val nextShot = value match {
      case Some("esquerda-cima") => Shot("direita-cima")
      case Some("direita-cima") => Shot("direita-baixo")
      case Some("direita-baixo") => Shot("esquerda-baixo")
      case Some("esquerda-baixo") => Shot("esquerda-cime")
      case Some(_) => Shot("esquerda-cime")
      case None => Shot("esquerda-cima")
    }
    IO(nextShot)
  }
}
