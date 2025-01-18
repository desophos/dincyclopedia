package dincyclopedia.ui

import dincyclopedia.model.*

import calico.*
import calico.html.io.*
import calico.html.io.given
import cats.effect.*
import fs2.concurrent.Signal
import fs2.dom.*
import org.legogroup.woof.Logger

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
