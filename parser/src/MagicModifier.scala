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
import org.legogroup.woof.given
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
        ScalingStat(levelBase, levelPerLevel, Bonus.Flat),
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
        .map((k, v) => (k.stripSuffix(suffix), v.toDouble))

    val baseStats     = getStats("Base")
    val perLevelStats = getStats("PerLevel")

    val statsWithErrors: List[OptionT[IO, (String, ScalingStat)]] =
      baseStats.toList.map((baseK, baseV) =>
        Bonus
          .fromKeyword(baseK)
          .map(bonus =>
            (
              Parsable.statPrefixPattern.replaceFirstIn(baseK, ""),
              ScalingStat(baseV, perLevelStats(baseK), bonus),
            )
          )
          .fold(
            Logger[IO]
              .warn("Failed to parse bonus from stat")
              .withContext(Keyword(baseK), Value(baseV.show))
              .as(None)
              .optionT
          )(OptionT.some[IO](_))
      )

    val leveledPairsWithErrors
        : List[OptionT[IO, (Int, model.MagicModifier.AtLevel)]] =
      leveledEntries.toList.map((leveledTitle, leveledKeywords) =>
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

    OptionT(
      for { // IO
        proc         <- Proc(keywords).value
        stats        <- statsWithErrors.unNone
        leveledPairs <- leveledPairsWithErrors.unNone
        maybePrefix  <- parseKeyword[Boolean](keywords, "Prefix").value
        maybeSpawnChance <- parseKeywordOrElse[Double](
          keywords,
          "SpawnChance",
          1.0,
        ).value
      } yield for { // Option
        prefix      <- maybePrefix
        spawnChance <- maybeSpawnChance
      } yield model.MagicModifier(
        prefix,
        magicRequirement,
        itemTypeRequirement,
        cursed,
        ego,
        spawnChance,
        proc,
        stats.toMap,
        SortedMap.from(leveledPairs),
      )
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
