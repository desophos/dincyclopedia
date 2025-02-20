package dincyclopedia.parser
package test

import scala.collection.MapView

import cats.data.NonEmptyList
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class ParsableSuite extends ScalaCheckSuite {
  property("groupEntriesByTitle when all entries have the same title") {
    forAll(sameTitleEntries) { (entries: NonEmptyList[ParsedEntry]) =>
      val groupedEntries: MapView[String, NonEmptyList[Map[String, String]]] =
        Parsable.groupEntriesByTitle(entries)

      assert(groupedEntries.nonEmpty)
      assertEquals(1, groupedEntries.keys.size)
      assertEquals(entries.length, groupedEntries.values.map(_.length).sum)
    }
  }

  property("groupEntriesByTitle when all entries have different titles") {
    forAll(differentTitleEntries) { (entries: NonEmptyList[ParsedEntry]) =>
      val groupedEntries: MapView[String, NonEmptyList[Map[String, String]]] =
        Parsable.groupEntriesByTitle(entries)

      assert(groupedEntries.nonEmpty)
      assertEquals(entries.length, groupedEntries.keys.size)
      groupedEntries.values.tapEach(entryList =>
        assertEquals(1, entryList.length)
      )
      assertEquals(entries.length, groupedEntries.values.map(_.length).sum)
    }
  }

  property("combineSameTitleEntries") {
    forAll(entriesByTitleWithMatchingKeyword) {
      (
          keyword: String,
          lastValue: String,
          entries: MapView[String, NonEmptyList[Map[String, String]]],
      ) =>
        val combinedEntries: MapView[String, Map[String, String]] =
          Parsable.combineSameTitleEntries(entries)

        assert(combinedEntries.nonEmpty)
        assertEquals(entries.keys.size, combinedEntries.keys.size)
        assertEquals(lastValue, combinedEntries.values.head(keyword))
    }
  }
}
