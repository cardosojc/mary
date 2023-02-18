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
      val result = Redis[IO].utf8("redis://default:48a16b0d4c38430f85c46db8e744194d@fly-marydb.upstash.io").use { redis =>
        for {
          x <- redis.get("lastshot")
        } yield x match {
          case Some(value) =>
            val next = calculateNextShot(value)
            redis.set("lastshot", next.place)
            next
          case None =>
            val next = "esquerda-cima"
            redis.set("lastshot", next)
            Shot(next)
        }
      }
      result
    }
  }

  private def calculateNextShot(value: String) = {
    value match {
      case "esquerda-cima" => Shot("direita-cima")
      case "direita-cima" => Shot("direita-baixo")
      case "direita-baixo" => Shot("esquerda-baixo")
      case "esquerda-baixo" => Shot("esquerda-cime")
      case _ => Shot("esquerda-cima")
    }
  }
}
