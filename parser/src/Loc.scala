package dincyclopedia.parser

import scala.util.matching.Regex

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.parse.Parser
import org.legogroup.woof.Logger
import os.SubPath

case class Loc private (loc: String) extends Entry

object Loc extends Parsable[Loc] {
  override val path = SubPath("""Loc\English""")

  override val ext = Some("trn")

  override def readFiles: IO[String] = {
    val spinAttackError: Regex    = """(?m)^Spin Attack""".r
    val nestedDoubleQuotes: Regex = """(?m)^.+?"|" *[\t\r\n]+|(\\?")""".r

    super.readFiles.map(text =>
      nestedDoubleQuotes.replaceSomeIn(
        spinAttackError.replaceAllIn(text, "SpinAttack"),
        m => if m.group(1) == null then None else Some("'"),
      )
    )
  }

  override def parser(using
      Logger[IO]
  ): Parser[OptionT[IO, Map[String, Loc]]] =
    (blankLines0.backtrack.with1 *> keywordLine <* blankLines0.backtrack).rep
      .map(
        _.toList.toMap.view
          .mapValues(_.replace('\'', '"'))
          .mapValues(Loc.apply)
          .toMap
      )
      .map(OptionT.some.apply)
}
