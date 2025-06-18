package dincyclopedia.ui

import dincyclopedia.model.*
import dincyclopedia.model.given

import calico.*
import calico.html.io.*
import cats.data.OptionT
import cats.effect.*
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

  def apply(baseUrl: String)(using Logger[IO]): Resource[IO, DataStore] = (for {
    magicModifiers <- getData[MagicModifier](baseUrl)
    leveledMagicModifiers = magicModifiers
      .map { (name, value) => value.leveled.map(_.name).zip(value.leveled) }
      .flatten
      .toMap
  } yield new DataStore(leveledMagicModifiers))
    .getOrRaise(new RuntimeException("Failed to construct DataStore"))
    .toResource
}
