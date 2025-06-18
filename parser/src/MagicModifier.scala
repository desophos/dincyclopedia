package dincyclopedia.parser

import scala.collection.View
import scala.collection.immutable.SortedMap

import dincyclopedia.model
import dincyclopedia.model.*

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.parse.Parser
import org.legogroup.woof.Logger
import os.SubPath

object MagicModifier {
  object Proc {
    def apply(
        keywords: Map[String, String]
    )(using Logger[IO]): OptionT[IO, model.MagicModifier.Proc] = {
      for {
        skill         <- OptionT fromOption keywords.get("OnHitSkill")
        chance        <- parseKeyword[Double](keywords, "SkillChance")
        levelBase     <- parseKeyword[Double](keywords, "SkillLevelBase")
        levelPerLevel <- parseKeyword[Double](keywords, "SkillLevelPerLevel")
      } yield model.MagicModifier.Proc(
        skill,
        chance,
        ScalingStat(levelBase, levelPerLevel),
      )
    }
  }

  object Leveled {
    def apply(
        baseName: Option[String],
        keywords: Map[String, String],
    )(using Logger[IO]): OptionT[IO, model.MagicModifier.AtLevel] = {
      val availableAtMaxLevel = keywords.get("AvailableAtMaxLevel").isDefined
      for {
        requirementsMult <- parseKeywordOrElse[Double](
          keywords,
          "RequirementsMult",
          1.0,
        )
      } yield model.MagicModifier.AtLevel(
        keywords.getOrElse("Name", baseName.getOrElse("[No Display Name]")),
        requirementsMult,
        availableAtMaxLevel,
      )
    }
  }

  def apply(
      keywords: Map[String, String],
      leveledEntries: View[(String, Map[String, String])],
  )(using Logger[IO]): OptionT[IO, model.MagicModifier] = {
    val name                = keywords.get("Name")
    val magicRequirement    = keywords.get("MagicRequirement")
    val itemTypeRequirement = keywords.get("ItemTypeRequirement")
    val cursed              = keywords.get("Cursed").isDefined
    val ego                 = keywords.get("Ego").isDefined

    def getStats(suffix: String): SortedMap[String, Double] =
      SortedMap from keywords
        .filter((k, _) =>
          k != suffix && !k.startsWith("SkillLevel") && k.endsWith(suffix)
        )
        .map((k, v) =>
          (
            Parsable.statPrefixes
              .map(k.stripPrefix)
              .find(_ != k)
              .getOrElse(k)
              .stripSuffix(suffix),
            v.toDouble,
          )
        )

    val baseStats     = getStats("Base")
    val perLevelStats = getStats("PerLevel")

    val stats = baseStats
      .map((baseK, baseV) => (baseK, ScalingStat(baseV, perLevelStats(baseK))))
      .unsorted

    OptionT(
      for {
        proc <- Proc(keywords).value
        leveledPairs <- leveledEntries.toList
          .map((leveledTitle, leveledKeywords) =>
            parseKeywordOrElse[Boolean](
              leveledKeywords,
              "BaseOnly",
              false,
            ).ifM(
              OptionT.none, // BaseOnly 1 means not a real leveled entry
              for {
                itemLevel <- parseKeywordOrElse[Int](
                  leveledKeywords,
                  "ItemLevel",
                  0,
                )
                leveled <- Leveled(name, leveledKeywords)
              } yield (itemLevel, leveled),
            ).withContext(LeveledTitle(leveledTitle))
          )
          .unNone
        magicModifier <- (
          for {
            prefix <- parseKeyword[Boolean](keywords, "Prefix")
            spawnChance <- parseKeywordOrElse[Double](
              keywords,
              "SpawnChance",
              1.0,
            )
          } yield model.MagicModifier(
            prefix,
            magicRequirement,
            itemTypeRequirement,
            cursed,
            ego,
            spawnChance,
            proc,
            stats,
            SortedMap.from(leveledPairs),
          )
        ).value
      } yield magicModifier
    )
  }
}

given Parsable[model.MagicModifier] with {
  val blacklist =
    List("EnhancementAll", "EnhancementLivingOnly") // abstract base modifiers

  override val path = SubPath("""Database\MagicModifiers""")

  override def parser(using
      Logger[IO]
  ): Parser[OptionT[IO, Map[String, model.MagicModifier]]] =
    entries
      .map(Parsable.groupEntries)
      .map { entriesByBase =>
        entriesByBase(Some("BaseMagicModifier")).toList
          .traverse((title, keywords) =>
            MagicModifier(
              keywords,
              entriesByBase(Some(title)), // .map((_, keywords) => keywords),
            ).withContext(Title(title))
              .tupleLeft(title.stripPrefix("BaseModifier"))
          )
          .map(_.filterNot((name, _) => blacklist.contains(name)).toMap)
      }
}
