package dincyclopedia.parser

import dincyclopedia.model.*
import dincyclopedia.model.given

import cats.data.OptionT
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import io.circe.Encoder
import org.legogroup.woof.Logger

object JsonParser extends IOApp {
  def makePath[A: JsonStorage] =
    os.pwd / "data" / s"${JsonStorage[A].filename}.json"

  def writeFile[A <: Entry: Parsable: Encoder: JsonStorage](using Logger[IO]) =
    Parsable[A].filesToJson.map(json =>
      os.write.over(
        makePath[A],
        json,
        createFolders = true,
      )
    )

  def writeJson: OptionT[IO, ExitCode] = for {
    logger <- OptionT liftF makeConsoleLogger
    given Logger[IO] = logger
    _ <- writeFile[Loc]
    _ <- writeFile[MagicModifier]
  } yield ExitCode.Success

  def run(args: List[String]): IO[ExitCode] =
    writeJson.getOrElse(ExitCode.Error)
}
