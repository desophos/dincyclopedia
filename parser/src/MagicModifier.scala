package soldaktools

import scala.collection.View
import scala.collection.immutable.SortedMap

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.parse.Parser
import org.legogroup.woof.Logger
import os.SubPath

case class MagicModifier private (
    prefix: Boolean,
    magicRequirement: Option[String],
    itemTypeRequirement: Option[String],
    cursed: Boolean,
    ego: Boolean,
    spawnChance: Double,
    proc: Option[MagicModifier.Proc],
    stats: Map[String, MagicModifier.StatValue],
    levels: Map[Int, MagicModifier.Leveled],
) extends Entry

object MagicModifier extends Parsable[MagicModifier] {
  case class StatValue(base: Double, perLevel: Double)

  case class Proc private (
      skill: String,
      chance: Double,
      level: StatValue,
  )

  object Proc {
    def apply(
        keywords: Map[String, String]
    )(using Logger[IO]): OptionT[IO, Proc] = {
      for {
        skill         <- OptionT fromOption keywords.get("OnHitSkill")
        chance        <- parseKeyword[Double](keywords, "SkillChance")
        levelBase     <- parseKeyword[Double](keywords, "SkillLevelBase")
        levelPerLevel <- parseKeyword[Double](keywords, "SkillLevelPerLevel")
      } yield Proc(skill, chance, StatValue(levelBase, levelPerLevel))
    }
  }

  case class Leveled private (
      name: String,
      requirementsMult: Double = 1.0,
      availableAtMaxLevel: Boolean = false,
  )

  object Leveled {
    def apply(
        baseName: Option[String],
        keywords: Map[String, String],
    )(using Logger[IO]): OptionT[IO, Leveled] = {
      val availableAtMaxLevel = keywords.get("AvailableAtMaxLevel").isDefined
      val name = keywords.getOrElse("Name", baseName.get) // One of these should always be defined
      parseKeywordOrElse[Double](
        keywords,
        "RequirementsMult",
        1.0,
      ).map(requirementsMult =>
        Leveled(name, requirementsMult, availableAtMaxLevel)
      )
    }
  }

  def apply(
      keywords: Map[String, String],
      leveledEntries: View[(String, Map[String, String])],
  )(using Logger[IO]): OptionT[IO, MagicModifier] = {
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
            k.stripPrefix("StatChange")
              .stripPrefix("DynamicStatMult")
              .stripSuffix(suffix),
            v.toDouble,
          )
        )

    val baseStats     = getStats("Base")
    val perLevelStats = getStats("PerLevel")

    val stats = baseStats
      .map((baseK, baseV) => (baseK, StatValue(baseV, perLevelStats(baseK))))
      .unsorted

    Proc(keywords).value
      .map(proc =>
        for {
          prefix <- parseKeyword[Boolean](keywords, "Prefix")
          spawnChance <- parseKeywordOrElse[Double](
            keywords,
            "SpawnChance",
            1.0,
          )
          leveledPairs <- leveledEntries.toList
            .map { (leveledTitle, leveledKeywords) =>
              parseKeywordOrElse[Boolean](
                leveledKeywords,
                "BaseOnly",
                false,
              ).ifM(
                OptionT.none, // BaseOnly 1 means not a real leveled entry
                for {
                  itemLevel <- parseKeyword[Int](
                    leveledKeywords,
                    "ItemLevel",
                  )
                  leveled <- Leveled(name, leveledKeywords)
                } yield (itemLevel, leveled),
              ).withContext(LeveledTitle(leveledTitle))
            }
            .unNone
            .sequence
        } yield MagicModifier(
          prefix,
          magicRequirement,
          itemTypeRequirement,
          cursed,
          ego,
          spawnChance,
          proc,
          stats,
          leveledPairs.toMap,
        )
      )
      .map(_.value)
      .flatten
      .optionT
  }

  override val path = SubPath("""Database\MagicModifiers""")

  override def parser(using
      Logger[IO]
  ): Parser[OptionT[IO, Map[String, MagicModifier]]] =
    entries
      .map {
        _.groupMap { case ((title, relationshipPair), _) =>
          relationshipPair match {
            case None                         => title
            case Some(relationship, original) => original
          }
        } { (_, keywords) => keywords }.view
          .mapValues(sameTitleEntries => sameTitleEntries.reduceLeft(_ ++ _))
          .groupBy((_, keywords) => keywords.get("Base"))
      }
      .map { entriesByBase =>
        entriesByBase(Some("BaseMagicModifier")).toList
          .traverse((title, keywords) =>
            MagicModifier(
              keywords,
              entriesByBase(Some(title)), // .map((_, keywords) => keywords),
            ).withContext(Title(title)) tupleLeft title
              .stripPrefix("BaseModifier")
          )
          .map(_.toMap)
      }
}
