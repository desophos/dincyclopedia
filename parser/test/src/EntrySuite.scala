package dincyclopedia.parser
package test

import MagicModifier.StatValue
import cats.effect.IO
import cats.effect.Resource
import org.legogroup.woof.*
import weaver.*

object EntrySuite extends IOSuite {
  override type Res = Logger[IO]
  override def sharedResource: Resource[IO, Res] =
    DefaultLogger.makeIo(consoleOutput).toResource

  def parsedSuccessfully[A <: Entry](p: Parsable[A])(logger: Logger[IO]) = {
    given Logger[IO] = logger
    for {
      data <- p.parseFiles.value
    } yield {
      expect(data.exists(_.nonEmpty))
    }
  }

  extension (m: MagicModifier) {
    def hasStat(name: String, base: Double, perLevel: Double) =
      m.stats.exists((k, v) =>
        k == name && v.base == base && v.perLevel == perLevel
      )
  }

  test("Loc parsed successfully")(parsedSuccessfully(Loc))

  test("MagicModifiers parsed successfully")(parsedSuccessfully(MagicModifier))

  test("AttackAndDamageMult parsed correctly") { logger =>
    given Logger[IO] = logger
    for {
      data <- MagicModifier.parseFiles.value
    } yield {
      expect(
        data.exists { ms =>
          val m = ms("AttackAndDamageMult")
          m.prefix == false
          && m.hasStat("DamageMult", 0.1, 0.015)
          && m.hasStat("Attack", 5.0, 3.0)
        }
      )
    }
  }

  test("ProcArcaneSwarm parsed correctly") { logger =>
    given Logger[IO] = logger
    for {
      data <- MagicModifier.parseFiles.value
    } yield {
      expect(data.exists { ms =>
        val m = ms("ProcArcaneSwarm")
        m.prefix == false
        && m.spawnChance == 0.25
        && m.magicRequirement.exists(_ == "Weapon")
        && m.hasStat("Value", 7, 7)
        && m.proc.exists(p =>
          p.skill == "SkillItemProcArcaneSwarm"
            && p.chance == 0.075
            && p.level == StatValue(1, 0.2534)
        )
      })
    }
  }
}
