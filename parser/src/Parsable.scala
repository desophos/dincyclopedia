package dincyclopedia.parser

import scala.collection.MapView
import scala.collection.View
import scala.collection.mutable.StringBuilder

import dincyclopedia.model.*

import cats.Semigroup
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.parse.Parser
import io.circe.Encoder
import io.circe.syntax.*
import org.legogroup.woof.Logger
import os.Path
import os.SubPath

trait Parsable[A <: Entry] {
  def path: SubPath

  def filename: Option[String] = None

  def ext: Option[String] = None

  def parser(using Logger[IO]): Parser[OptionT[IO, Map[String, A]]]

  def readFiles: IO[String] = IO.blocking(
    os.walk(Parsable.basePath)
      .filter(_.dropLast endsWith path)
      .filter(path => filename.fold(true)(_ == path.baseName))
      .filter(path => ext.fold(true)(_ == path.ext))
      .map(os.read)
      .foldLeft(StringBuilder())(_ ++= _)
      .result
  )

  final def parseFiles(using Logger[IO]): OptionT[IO, Map[String, A]] =
    OptionT
      .liftF(readFiles)
      .flatMap(parser.parseAll(_).logLeftToOption.flatten)

  final def filesToJson(using Logger[IO], Encoder[A]): OptionT[IO, String] =
    parseFiles.map(_.asJson.noSpacesSortKeys)

  final def keywordTypeParser: Parser[Map[String, String]] = entries.map(
    _.map {
      _.keywords.view
        .filterKeys(k =>
          !Parsable.statPrefixes.exists(prefix => k.startsWith(prefix))
        )
        .mapValues(List(_)) // for combination by Semigroup
        .toMap
    }
      .reduce(using Semigroup[Map[String, List[String]]])
      .view
      .mapValues {
        _ match { // order is important; narrower matches come first
          case vs if vs.forall(Parsable.booleanPattern.matches) => "Boolean"
          case vs if vs.forall(Parsable.intPattern.matches)     => "Int"
          case vs if vs.forall(Parsable.doublePattern.matches)  => "Double"
          case _                                                => "String"
        }
      }
      .toMap
  )

  final def showKeywordTypes(using Logger[IO]): OptionT[IO, String] =
    for {
      fileText <- OptionT.liftF(readFiles)
      keywords <- keywordTypeParser.parseAll(fileText).logLeftToOption
    } yield {
      keywords
        .map((keyword, typeName) => s"${keyword.uncapitalize}: $typeName")
        .mkString("\n")
    }
}

object Parsable {
  def apply[A <: Entry](using p: Parsable[A]): Parsable[A] = p

  def groupEntriesByTitle(
      parsedEntries: NonEmptyList[ParsedEntry]
  ): MapView[String, NonEmptyList[Map[String, String]]] =
    parsedEntries
      .groupMap(entry =>
        entry.parent match {
          case None         => entry.title
          case Some(parent) => parent.title
        }
      )(_.keywords)
      .view

  // Take the latest value of every keyword.
  def combineSameTitleEntries(
      entriesByTitle: MapView[String, NonEmptyList[Map[String, String]]]
  ): MapView[String, Map[String, String]] =
    entriesByTitle.mapValues(sameTitleEntries =>
      sameTitleEntries.reduceLeft(_ ++ _)
    )

  def groupEntriesByBase(
      combinedEntries: MapView[String, Map[String, String]]
  ): Map[
    Option[String],
    View[(String, Map[String, String])],
  ] =
    combinedEntries.groupBy((_, keywords) => keywords.get("Base"))

  val groupEntries: NonEmptyList[ParsedEntry] => Map[
    Option[String],
    View[(String, Map[String, String])],
  ] =
    groupEntriesByTitle andThen combineSameTitleEntries andThen groupEntriesByBase

  private val basePath = Path(
    """C:\Users\desop\personal\for games\soldak\Din's Legacy"""
  )

  private val booleanPattern = """^(0|1)$""".r
  private val intPattern     = """^-?\d+$""".r
  private val doublePattern  = """^-?\d+(\.\d+)?$""".r

  final val statPrefixes = List(
    "DefendingDynamicStatChange",
    "DefendingDynamicStatMult",
    "PassiveDynamicStatChange",
    "PassiveDynamicStatMult",
    "DynamicStatChange",
    "DynamicStatMult",
    "StatChange",
    "StatMult",
  )

  final val statPrefixPattern = s"^(${statPrefixes.mkString("|")})".r
}
