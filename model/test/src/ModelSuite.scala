package dincyclopedia.model.test

import dincyclopedia.model.*
import dincyclopedia.model.given

import io.circe.testing.CodecTests
import io.circe.testing.instances.*
import io.github.martinhh.derived.scalacheck.given
import munit.DisciplineSuite

class ModelSuite extends DisciplineSuite {
  checkAll("Loc", CodecTests[Loc].codec)
  checkAll("MagicModifier", CodecTests[MagicModifier].codec)
  checkAll("Skill", CodecTests[Skill].codec)
}
