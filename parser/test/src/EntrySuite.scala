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
    def hasStat(name: String, base: Double, perLevel: Double, bonus: Bonus) =
      m.stats.exists((k, v) =>
        k == name && v.base == base && v.perLevel == perLevel && v.bonus == bonus
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

  test("Leveled MagicModifiers parsed successfully") {
    given Logger[IO] = logger()
    for {
      data <- Parsable[MagicModifier].parseFiles.value
    } yield {
      assert(data.exists(_.nonEmpty))
      assert(
        data.get.values.forall(_.leveled.nonEmpty),
        data.get.filter((_, v) => v.leveled.isEmpty).keys,
      )
    }
  }

  test("AttackAndDamageMult parsed correctly") {
    for {
      data <- Parsable[MagicModifier].parseFiles(using logger()).value
    } yield {
      assert(data.exists(_.contains("AttackAndDamageMult")))
      val m = data.get("AttackAndDamageMult")
      assert(!m.prefix)
      assert(m.hasStat("DamageMult", 0.1, 0.015, Bonus.Mult))
      assert(m.hasStat("Attack", 5.0, 3.0, Bonus.Flat))
      assert(m.levels.nonEmpty)
      assert(m.leveled.nonEmpty)
      val m4 = m.leveled(3)
      assertEquals(m4.name, "$$AttackAndDamageMultModifierName4$$")
      assert(!m4.availableAtMaxLevel)
      assert(!m4.cursed)
      assert(!m4.ego)
      assert(m4.itemTypeRequirement.isEmpty)
      assertEquals(m4.magicRequirement, Some("Weapon"))
      assert(!m4.prefix)
      assert(m4.proc.isEmpty)
      assertEqualsDouble(m4.requirementsMult, 1.0, 0.00001)
      assertEqualsDouble(m4.spawnChance, 1.0, 0.00001)
      assert(m4.stats.nonEmpty)
      assertEqualsDouble(m4.stats("DamageMult"), 0.325, 0.00001)
      assertEqualsDouble(m4.stats("Attack"), 50.0, 0.00001)
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
        && m.hasStat("Value", 7, 7, Bonus.Flat)
        && m.proc.exists(p =>
          p.skill == "SkillItemProcArcaneSwarm"
            && p.chance == 0.075
            && p.level == ScalingStat(1, 0.2534, Bonus.Flat)
        )
      })
    }
  }
}
