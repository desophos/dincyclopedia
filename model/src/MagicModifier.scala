package dincyclopedia.model

import scala.collection.immutable.SortedMap

import cats.Eq
import cats.derived.*

case class MagicModifier(
    prefix: Boolean,
    magicRequirement: Option[String],
    itemTypeRequirement: Option[String],
    cursed: Boolean,
    ego: Boolean,
    spawnChance: Double,
    proc: Option[MagicModifier.Proc],
    stats: Map[String, ScalingStat],
    levels: SortedMap[Int, MagicModifier.AtLevel],
) extends Entry
    derives Eq {
  def leveled: List[LeveledMagicModifier] = levels
    .map((level, atLevel) =>
      LeveledMagicModifier(
        level,
        atLevel.name,
        prefix,
        magicRequirement,
        itemTypeRequirement,
        cursed,
        ego,
        spawnChance,
        proc.map(p =>
          LeveledMagicModifier.Proc(
            p.skill,
            p.chance,
            (p.level.base + p.level.perLevel * level).round.toInt,
          )
        ),
        stats.view.mapValues(s => s.base + s.perLevel * level).toMap,
        atLevel.requirementsMult,
        atLevel.availableAtMaxLevel,
      )
    )
    .toList
}

object MagicModifier {
  case class Proc(
      skill: String,
      chance: Double,
      level: ScalingStat,
  ) derives Eq

  case class AtLevel(
      name: String,
      requirementsMult: Double = 1.0,
      availableAtMaxLevel: Boolean = false,
  ) derives Eq
}

case class LeveledMagicModifier(
    level: Int,
    name: String,
    prefix: Boolean,
    magicRequirement: Option[String],
    itemTypeRequirement: Option[String],
    cursed: Boolean,
    ego: Boolean,
    spawnChance: Double,
    proc: Option[LeveledMagicModifier.Proc],
    stats: Map[String, Double],
    requirementsMult: Double,
    availableAtMaxLevel: Boolean,
)

object LeveledMagicModifier {
  case class Proc(
      skill: String,
      chance: Double,
      level: Int,
  )
}
