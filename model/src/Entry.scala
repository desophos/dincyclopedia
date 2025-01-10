package dincyclopedia.model

trait Entry {
  def base: Option[Entry] = None
  def baseOnly: Boolean   = false
}
