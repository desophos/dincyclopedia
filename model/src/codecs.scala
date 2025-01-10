package dincyclopedia.model

import io.circe.*
import io.circe.generic.semiauto.*

given Codec[ScalingStat]           = deriveCodec
given Codec[Loc]                   = deriveCodec
given Codec[MagicModifier.Proc]    = deriveCodec
given Codec[MagicModifier.AtLevel] = deriveCodec
given Codec[MagicModifier]         = deriveCodec
given Codec[Skill]                 = deriveCodec
