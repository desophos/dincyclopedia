package dincyclopedia.model

import cats.Eq
import cats.derived.*

case class Loc(loc: String) extends Entry derives Eq

object Loc extends JsonStorage {
  override val filename = "loc"
}
