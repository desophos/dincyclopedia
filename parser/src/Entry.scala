package soldaktools

import scala.collection.mutable.StringBuilder

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.parse.Parser
import org.legogroup.woof.Logger
import os.Path
import os.SubPath

trait Entry {
  def base: Option[Entry] = None
  def baseOnly: Boolean   = false
}

trait Parsable[A <: Entry] {
  def path: SubPath

  def ext: Option[String] = None

  def parser(using Logger[IO]): Parser[OptionT[IO, Map[String, A]]]

  def readFiles: IO[String] = IO.blocking(
    os.walk(Parsable.basePath)
      .filter(_.dropLast endsWith path)
      .filter(path => path.ext == ext.getOrElse(path.ext))
      .map(os.read)
      .foldLeft(StringBuilder())(_ ++= _)
      .result
  )

  def parseFiles(using Logger[IO]): OptionT[IO, Map[String, A]] =
    OptionT
      .liftF(readFiles)
      .flatMap(parser.parseAll(_).logLeftToOption.flatten)
}

object Parsable {
  private val basePath = Path(
    """C:\Users\desop\personal\for games\soldak\Din's Legacy"""
  )
}

extension (path: Path) {
  def dropLast: Path = os.root / path.segments.toList.init
  def contains(sub: SubPath): Boolean =
    path.segments.sliding(sub.segments.size).contains(sub.segments)
}
