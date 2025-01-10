package dincyclopedia.model

trait JsonStorage[A] {
  val filename: String
}

object JsonStorage {
  def apply[A](using p: JsonStorage[A]): JsonStorage[A] = p
}
