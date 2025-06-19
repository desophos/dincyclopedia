package dincyclopedia.ui

import scala.scalajs.js.Thenable

import dincyclopedia.model.*

import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import io.circe.Json
import io.circe.parser.parse
import org.legogroup.woof.Logger
import org.legogroup.woof.given
import org.scalajs.dom.Response
import org.scalajs.dom.fetch

def boolean2option(b: Boolean): Option[Boolean] = Option.when(b)(b)

extension [A](p: Thenable[A]) {
  def toIO: IO[A] = IO.fromFuture(IO(p.toFuture))
}

extension (res: Response) {
  def isOk(using Logger[IO]): OptionT[IO, Response] = OptionT(
    if res.ok then res.some.pure
    else Logger[IO].error(f"${res.status} ${res.statusText}").as(None)
  )

  def bodyText(using Logger[IO]): OptionT[IO, String] =
    res.isOk.semiflatMap(_.text().toIO)

  def bodyJson(using Logger[IO]): OptionT[IO, Json] =
    res.bodyText.map(parse).flatMap(_.logLeftToOption)
}

def fetchResponse(url: String)(using Logger[IO]): OptionT[IO, Response] =
  OptionT.liftF(fetch(url).toIO)

def fetchResponseText(url: String)(using Logger[IO]): OptionT[IO, String] =
  fetchResponse(url).flatMap(_.bodyText)

def fetchResponseJson(url: String)(using Logger[IO]): OptionT[IO, Json] =
  fetchResponse(url).flatMap(_.bodyJson)
