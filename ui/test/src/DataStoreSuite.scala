package dincyclopedia.ui
package test

import dincyclopedia.model.*

import cats.effect.IO
import munit.CatsEffectSuite
import org.legogroup.woof.*

class DataStoreSuite extends CatsEffectSuite {
  val logger = ResourceSuiteLocalFixture(
    "logger",
    makeConsoleLogger.toResource,
  )

  val dataStore = ResourceSuiteLocalFixture(
    "dataStore",
    makeConsoleLogger.toResource.flatMap(
      DataStore("https://raw.githubusercontent.com/desophos/dincyclopedia/refs/heads/main/data/")(
        using _
      )
    ),
  )

  override def munitFixtures = List(logger, dataStore)

  test("MagicModifiers retrieved successfully") {
    given Logger[IO] = logger()
    val data         = dataStore()
    assert(data.magicModifiers.nonEmpty)
  }
}
