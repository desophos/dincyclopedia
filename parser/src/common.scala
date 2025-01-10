package dincyclopedia.parser

import cats.Show
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.parse.Parser as P
import cats.parse.Parser0 as P0
import cats.parse.Rfc5234.alpha
import cats.parse.Rfc5234.digit
import cats.parse.Rfc5234.vchar
import org.legogroup.woof.*

sealed trait ParsingError
case class KeywordNotFoundError(keyword: String)        extends ParsingError
case class MassageError(keyword: String, value: String) extends ParsingError

given Show[KeywordNotFoundError] = Show.show(e =>
  show"Keyword ${e.keyword} not found in entry. Parser needs to be changed to consider this keyword optional."
)

given Show[MassageError] = Show.show(e =>
  show"${e.value} could not be massaged to fit keyword ${e.keyword}. Type coercion failed; check the keyword's type."
)

given Show[ParsingError] = Show.show(_ match {
  case e: KeywordNotFoundError => e.show
  case e: MassageError         => e.show
})

enum Relationship(val s: String) {
  case overrides extends Relationship("overrides")
  case addsTo    extends Relationship("addsTo")
}

case class RelationshipTo(relationship: Relationship, title: String)

case class ParsedEntry(
    title: String,
    parent: Option[RelationshipTo],
    keywords: Map[String, String],
)

// Parsers

val hspace: P[Unit]     = P.charIn(" \t").void
val linebreak: P[Unit]  = P.char('\n') | P.string("\r\n")
val hspaces: P[Unit]    = hspace.rep.void
val linebreaks: P[Unit] = linebreak.rep.void

val openBrace: P[Unit]      = P.char('{').void
val closeBrace: P[Unit]     = P.char('}').void
val commentOneLine: P[Unit] = (P.string("//") *> P.until0(linebreak)).void
val commentMultiLine: P[Unit] =
  (P.string("/*") *> P.until0(P.string("*/")) *> P.string("*/")).void
val comment: P[Unit] = commentOneLine | commentMultiLine

val alphanum: P[Char] = alpha | digit | P.charIn("-_")
val word: P[String]   = alphanum.rep.string
val value: P0[String] =
  P.until0(P.char('"')).surroundedBy(P.char('"')) | vchar.rep.string
val keywordAndValue: P[(String, String)] =
  (word <* hspaces) ~ value
val relationshipWord: P[Relationship] =
  P.stringIn(Relationship.values.map(_.s)).map(Relationship.valueOf)

val blankLine: P[Unit]    = (hspaces.? ~ comment.?).with1 *> linebreak
val blankLines0: P0[Unit] = blankLine.backtrack.rep0.void

val openBraceLine: P[Unit]  = openBrace *> linebreak
val closeBraceLine: P[Unit] = closeBrace *> linebreak

val titleLine: P[(String, Option[(Relationship, String)])] =
  (word <* hspaces.?) ~ ((relationshipWord <* hspaces) ~ word).? <* linebreak

val keywordLine: P[(String, String)] =
  hspaces.?.with1 *> keywordAndValue <* hspaces.? <* comment.? <* linebreak

val entry: P[((String, Option[(Relationship, String)]), Map[String, String])] =
  titleLine ~ (blankLines0.with1 *> keywordLine <* blankLines0.backtrack)
    // .surroundedBy(blankLines0)
    .rep0 // entries can be empty
    .between(openBraceLine, closeBraceLine)
    .map(_.toMap)

val entries: P[
  NonEmptyList[((String, Option[(Relationship, String)]), Map[String, String])]
] = entry.surroundedBy(blankLines0).rep <* P.end

// Helpers

trait KeywordParser[T] {
  extension (value: String) {
    def parse(keyword: String): Either[MassageError, T]
  }
}

given KeywordParser[Boolean] with {
  extension (value: String) {
    def parse(keyword: String) = value match {
      case "0" => Right(false)
      case "1" => Right(true)
      case _   => Left(MassageError(keyword, value))
    }
  }
}

given KeywordParser[Double] with {
  extension (value: String) {
    def parse(keyword: String) =
      value.toDoubleOption.toRight(MassageError(keyword, value))
  }
}

given KeywordParser[Int] with {
  extension (value: String) {
    def parse(keyword: String) =
      value.toIntOption.toRight(MassageError(keyword, value))
  }
}

def parseKeyword[T: KeywordParser](
    keywords: Map[String, String],
    keyword: String,
)(using Logger[IO]): OptionT[IO, T] =
  keywords
    .get(keyword)
    .toRight(KeywordNotFoundError(keyword))
    .flatMap(_.parse(keyword))
    .logLeftToOption

def parseKeywordOrElse[T: KeywordParser](
    keywords: Map[String, String],
    keyword: String,
    default: T,
)(using Logger[IO]): OptionT[IO, T] =
  keywords
    .get(keyword)
    .fold(default.asRight)(_.parse(keyword))
    .logLeftToOption
