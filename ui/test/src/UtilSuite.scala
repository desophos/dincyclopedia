package dincyclopedia.ui
package test

import dincyclopedia.model.*

import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import munit.CatsEffectSuite
import org.legogroup.woof.*

class UtilSuite extends CatsEffectSuite {
  val logger = ResourceSuiteLocalFixture(
    "logger",
    makeConsoleLogger.toResource,
  )

  override def munitFixtures = List(logger)

  final val testUrl = "https://jsonplaceholder.typicode.com/posts/1"

  def checkResponse(response: Json): Boolean =
    response.asObject.get("id").get.asNumber.get.toInt.get == 1

  test("JSON fetched successfully") {
    given Logger[IO] = logger()
    for {
      resp <- fetchResponseJson(testUrl).value
    } yield {
      assert(resp.nonEmpty)
      assert(checkResponse(resp.get), resp.show)
    }
  }
}
