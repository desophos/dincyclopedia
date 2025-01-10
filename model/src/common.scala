package dincyclopedia.model

import cats.Eq
import cats.derived.*

case class ScalingStat(base: Double, perLevel: Double) derives Eq
