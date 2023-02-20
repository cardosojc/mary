package io.cardosojcs.mary

import cats.effect.{ExitCode, IO}
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import org.http4s.circe.{jsonDecoder, jsonEncoder, jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, EntityDecoder, EntityEncoder, Header, Headers, Method, Request, Uri}
import org.typelevel.ci.CIString

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

  final case class TodoistTask(content: String, project_id: String,  due_string: String, priority: Int)

  implicit val todoistTaskDecoder: Decoder[TodoistTask] = deriveDecoder[TodoistTask]
  implicit val todoistTaskEncoder: Encoder[TodoistTask] = deriveEncoder[TodoistTask]


  def impl(C: Client[IO]): DiabetesShot = new DiabetesShot {

    def createTodoistTask(next: Shot, dueDate: String): IO[ExitCode] = {
      val todoistTask = TodoistTask(s"Insulina Mary : ${next.place}", "2236403566", dueDate, 3)
      val authToken = sys.env.getOrElse("TODOIST_API_KEY", "")

      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString("https://api.todoist.com/rest/v2/tasks"),
        headers = Headers.apply(
          Authorization(Credentials.Token(AuthScheme.Bearer, authToken)),
          Header.Raw(CIString("Content-Type"), "application/json")
        )
      ).withEntity(todoistTask.asJson)

        for {
          response <- C.expect[Json](request)
          _ <- IO(println(s"Created task with ID: ${response.asJson}"))
        } yield ExitCode.Success
    }


    def get: IO[DiabetesShot.Shot] = {
      Redis[IO].utf8(sys.env.getOrElse("REDIS_URL", "redis://localhost")).use { redis =>
        for {
          x <- redis.get("lastshot")
          nextMorning <- calculateNextShot(x)
          nextEvening <- calculateNextShot(Option(nextMorning.place))
          _ <- redis.set("lastshot", nextEvening.place)
          _ <- createTodoistTask(nextMorning, "today at 10am")
          _ <- createTodoistTask(nextEvening, "today at 10pm")
        } yield nextEvening
      }
    }
  }

  private def calculateNextShot(value: Option[String]) = {
    val nextShot = value match {
      case Some("esquerda-cima") => Shot("direita-cima")
      case Some("direita-cima") => Shot("direita-baixo")
      case Some("direita-baixo") => Shot("esquerda-baixo")
      case Some("esquerda-baixo") => Shot("esquerda-cima")
      case Some(_) => Shot("esquerda-cima")
      case None => Shot("esquerda-cima")
    }
    IO(nextShot)
  }
}
