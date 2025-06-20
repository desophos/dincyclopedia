package dincyclopedia.ui

import dincyclopedia.model.*
import dincyclopedia.model.given

import calico.*
import calico.html.io.*
import cats.data.OptionT
import cats.effect.*
import cats.implicits.*
import io.circe.Decoder
import org.legogroup.woof.Logger
import org.legogroup.woof.given
import org.scalajs.dom.URL

class DataStore(val magicModifiers: Map[String, LeveledMagicModifier])

object DataStore {
  private def makeUrl[A: JsonStorage](baseUrl: String): URL =
    URL(f"${JsonStorage[A].filename}.json", baseUrl)

  private def getData[A <: Entry: Decoder: JsonStorage](baseUrl: String)(using
      Logger[IO]
  ): OptionT[IO, Map[String, A]] =
    fetchResponseJson(makeUrl[A](baseUrl).toString)
      .flatMap(_.as[Map[String, A]].logLeftToOption)
      .flatTapNone(
        Logger[IO].error(f"Couldn't get ${JsonStorage[A].filename} data")
      )

  private def getLoc(
      loc: Map[String, String]
  )(unlocalized: String)(using Logger[IO]): IO[String] = unlocalized
    .split(" ")
    .toList
    .traverse(key =>
      loc.get(key.stripPrefix("$$").stripSuffix("$$")) match {
        case Some(value) => IO.pure(value)
        case None =>
          Logger[IO]
            .warn(f"Missing localization")
            .withContext(Unlocalized(key)) *> IO.pure(key)
      }
    )
    .map(_.mkString(" "))

  def apply(baseUrl: String)(using Logger[IO]): Resource[IO, DataStore] = (for {
    loc <- getData[Loc](baseUrl).map(_.view.mapValues(_.loc).toMap)
    localize = getLoc(loc)
    magicModifiers <- getData[MagicModifier](baseUrl)
    leveledMagicModifiers <- OptionT.liftF(
      magicModifiers.toList
        .map { (name, mm) => mm.leveled.map(_.name).zip(mm.leveled) }
        .map(_.sortBy((_, mm) => mm.level))
        .flatten
        .traverse((name, mm) =>
          for {
            localizedName <- localize(name)
            localizedStats <- mm.stats.toList.traverse((statName, statValue) =>
              localize(statName).tupleRight(statValue)
            )
          } yield (localizedName, mm.copy(stats = localizedStats.toMap))
        )
        .map(_.toMap)
    )
  } yield new DataStore(leveledMagicModifiers))
    .getOrRaise(new RuntimeException("Failed to construct DataStore"))
    .toResource
}
