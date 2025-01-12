package dincyclopedia.ui

import dincyclopedia.model.*
import dincyclopedia.model.given

import calico.*
import calico.html.io.*
import calico.html.io.given
import cats.data.OptionT
import cats.effect.*
import fs2.concurrent.Signal
import fs2.dom.*
import io.circe.Decoder
import org.legogroup.woof.Logger
import org.legogroup.woof.given
// import fs2.concurrent.SignallingRef
import org.scalajs.dom.URL

object DincyclopediaApp extends IOWebApp {
  final val BASE_URL = "https://desophos.github.io/dincyclopedia"

  def render: Resource[IO, HtmlElement[IO]] = for {
    logger <- makeConsoleLogger.toResource
    given Logger[IO] = logger
    data <- DataStore.apply
    view <- div(
      children[String](name =>
        data.magicModifiers(name).toHtmlResource(name)
      ) <-- Signal.constant(
        data.magicModifiers.keys.toList
      )
    )
  } yield view

  def MagicModifierCard(
      m: LeveledMagicModifier
  ): Resource[IO, HtmlDivElement[IO]] =
    ???
}

class DataStore(val magicModifiers: Map[String, LeveledMagicModifier])

object DataStore {
  def makeUrl[A: JsonStorage]: URL =
    URL(f"${JsonStorage[A].filename}.json", DincyclopediaApp.BASE_URL)

  def getData[A <: Entry: Decoder: JsonStorage](using
      Logger[IO]
  ): OptionT[IO, Map[String, A]] =
    fetchResponseJson(makeUrl[A].toString)
      .flatMap(_.as[Map[String, A]].logLeftToOption)
      .flatTapNone(
        Logger[IO].error(f"Couldn't get ${JsonStorage[A].filename} data")
      )

  def apply(using Logger[IO]): Resource[IO, DataStore] = (for {
    magicModifiers <- getData[MagicModifier]
    leveledMagicModifiers = magicModifiers
      .map { (name, value) => value.leveled.map(_.name).zip(value.leveled) }
      .flatten
      .toMap
  } yield new DataStore(leveledMagicModifiers))
    .getOrRaise(new RuntimeException("Failed to construct DataStore"))
    .toResource
}
