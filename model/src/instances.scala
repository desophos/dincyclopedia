package dincyclopedia.model

import cats.Show

given JsonStorage[MagicModifier] with {
  override val filename = "magic_modifiers"
}

given JsonStorage[Loc] with {
  override val filename = "loc"
}

given JsonStorage[Skill] with {
  override val filename = "skills"
}

given Show[MagicModifier]        = Show.fromToString
given Show[LeveledMagicModifier] = Show.fromToString
