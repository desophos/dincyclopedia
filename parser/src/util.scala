package dincyclopedia.parser

import os.Path
import os.SubPath

extension (path: Path) {
  def dropLast: Path = os.root / path.segments.toList.init
  def contains(sub: SubPath): Boolean =
    path.segments.sliding(sub.segments.size).contains(sub.segments)
}
