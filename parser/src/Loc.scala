package dincyclopedia.parser

import scala.util.matching.Regex

import dincyclopedia.model
import dincyclopedia.model.Loc

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.parse.Parser
import org.legogroup.woof.Logger
import os.SubPath

given Parsable[model.Loc] with {
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
  ): Parser[OptionT[IO, Map[String, model.Loc]]] = {
    val whitespace: Regex              = """\s+""".r
    val formatCode: Regex              = """\^[a-z]\d\d\d""".r
    val newlineContinueSentence: Regex = """\\n""".r
    val newlineEndSentence: Regex      = """\.?\\n\\n""".r

    (blankLines0.backtrack.with1 *> keywordLine <* blankLines0.backtrack).rep
      .map(
        _.toList.toMap.view
          .mapValues(formatCode.replaceAllIn(_, "")) // Remove format codes
          .mapValues(newlineEndSentence.replaceAllIn(_, ". ")) // Replace these first so the next regex doesn't steal them
          .mapValues(newlineContinueSentence.replaceAllIn(_, " "))
          .mapValues(whitespace.replaceAllIn(_, " ").trim) // Remove extra whitespace
          .mapValues(model.Loc(_))
          .toMap
      )
      .map(OptionT.some.apply)
  }
}
