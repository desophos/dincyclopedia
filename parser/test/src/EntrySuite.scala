package dincyclopedia.parser
package test

import dincyclopedia.model.*

import cats.effect.IO
import munit.CatsEffectSuite
import org.legogroup.woof.*

class EntrySuite extends CatsEffectSuite {
  val logger = ResourceSuiteLocalFixture(
    "logger",
    makeConsoleLogger.toResource,
  )

  override def munitFixtures = List(logger)

  def parsedSuccessfully[A <: Entry: Parsable](using Logger[IO]) =
    for {
      data <- Parsable[A].parseFiles.value
    } yield {
      assert(data.exists(_.nonEmpty))
    }

  extension (m: MagicModifier) {
    def hasStat(name: String, base: Double, perLevel: Double) =
      m.stats.exists((k, v) =>
        k == name && v.base == base && v.perLevel == perLevel
      )
  }

  // test("Skill keyword types parsed") { logger =>
  //   given Logger[IO] = logger
  //   for {

  //   } yield {}
  // }

  test("Loc parsed successfully") {
    given Logger[IO] = logger()
    parsedSuccessfully[Loc]
  }

  test("MagicModifiers parsed successfully") {
    given Logger[IO] = logger()
    parsedSuccessfully[MagicModifier]
  }

  test("AttackAndDamageMult parsed correctly") {
    for {
      data <- Parsable[MagicModifier].parseFiles(using logger()).value
    } yield {
      assert(
        data.exists { ms =>
          val m = ms("AttackAndDamageMult")
          m.prefix == false
          && m.hasStat("DamageMult", 0.1, 0.015)
          && m.hasStat("Attack", 5.0, 3.0)
        }
      )
    }
  }

  test("ProcArcaneSwarm parsed correctly") {
    for {
      data <- Parsable[MagicModifier].parseFiles(using logger()).value
    } yield {
      assert(data.exists { ms =>
        val m = ms("ProcArcaneSwarm")
        m.prefix == false
        && m.spawnChance == 0.25
        && m.magicRequirement.exists(_ == "Weapon")
        && m.hasStat("Value", 7, 7)
        && m.proc.exists(p =>
          p.skill == "SkillItemProcArcaneSwarm"
            && p.chance == 0.075
            && p.level == ScalingStat(1, 0.2534)
        )
      })
    }
  }
}
