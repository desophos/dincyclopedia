package dincyclopedia.model.test

import dincyclopedia.model.*

import io.github.martinhh.derived.scalacheck.deriveArbitrary
import org.scalacheck.Arbitrary

given Arbitrary[Loc]           = deriveArbitrary
given Arbitrary[MagicModifier] = deriveArbitrary
given Arbitrary[Skill]         = deriveArbitrary
