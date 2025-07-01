package dincyclopedia.model

import cats.Eq
import cats.derived.*

enum Bonus {
  case Flat, Mult
  // "Defending" bonuses actually apply the stat to creatures who attack you.
  case AttackerFlat, AttackerMult
}

object Bonus {
  def fromKeyword(k: String): Option[Bonus] = {
    if (k.startsWith("Defending") && k.contains("StatChange"))
      Some(AttackerFlat)
    else if (k.startsWith("Defending") && k.contains("StatMult"))
      Some(AttackerMult)
    else if (k.contains("StatChange"))
      Some(Flat)
    else if (k.contains("StatMult"))
      Some(Mult)
    // special cases follow
    else if (k == "WeaponSpeed" || k.endsWith("Mult"))
      Some(Mult)
    else if (
      k == "Value" || k.endsWith("Damage")
      || k.endsWith("PerHit") || k.endsWith("PerHitTaken")
      || k.endsWith("PerKill") || k.endsWith("PerBlock")
    )
      Some(Flat)
    else None
  }
}

case class ScalingStat(base: Double, perLevel: Double, bonus: Bonus) derives Eq
