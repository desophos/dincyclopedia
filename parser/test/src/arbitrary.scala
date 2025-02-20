package dincyclopedia.parser.test

import scala.collection.MapView

import dincyclopedia.parser.ParsedEntry

import cats.data.NonEmptyList
import cats.implicits.*
import io.github.martinhh.derived.scalacheck.deriveArbitrary
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

given Arbitrary[ParsedEntry]               = deriveArbitrary
given Arbitrary[NonEmptyList[ParsedEntry]] = deriveArbitrary

val nonEmptyAsciiString = Gen.nonEmptyStringOf(Gen.asciiChar)

def uniqueListOfN[T](n: Int, g: Gen[T]): Gen[List[T]] = {

  def nonDuplicates[T](existing: List[T], toAdd: LazyList[T]): List[T] = {
    if existing.length >= n then existing
    else {
      val nextToAdd = toAdd.dropWhile(existing.contains)
      nonDuplicates(existing :+ nextToAdd.head, nextToAdd.tail)
    }
  }

  Gen.infiniteLazyList(g).map(nonDuplicates(List.empty, _))
}

def uniqueNelOfN[T](n: Int, g: Gen[T]): Gen[NonEmptyList[T]] = for {
  t  <- g
  ts <- uniqueListOfN(n - 1, g)
} yield NonEmptyList(t, ts)

val sameTitleEntries: Gen[NonEmptyList[ParsedEntry]] = for {
  e     <- Arbitrary.arbitrary[ParsedEntry]
  es    <- Gen.nonEmptyListOf(Arbitrary.arbitrary[ParsedEntry])
  title <- nonEmptyAsciiString
} yield NonEmptyList(e, es).map(_.copy(title = title, parent = None))

val differentTitleEntries: Gen[NonEmptyList[ParsedEntry]] = for {
  e  <- Arbitrary.arbitrary[ParsedEntry]
  es <- Gen.nonEmptyListOf(Arbitrary.arbitrary[ParsedEntry])
  nel = NonEmptyList(e, es)
  titles <- uniqueNelOfN(nel.length, nonEmptyAsciiString)
} yield nel.zipWith(titles)((e, title) => e.copy(title = title, parent = None))

val entriesByTitleWithMatchingKeyword: Gen[
  (String, String, MapView[String, NonEmptyList[Map[String, String]]])
] =
  for {
    title <- nonEmptyAsciiString
    e     <- Gen.nonEmptyMap(Gen.zip(nonEmptyAsciiString, nonEmptyAsciiString))
    es <- Gen.nonEmptyListOf(
      for {
        m <- Gen.nonEmptyMap(Gen.zip(nonEmptyAsciiString, nonEmptyAsciiString))
        v <- nonEmptyAsciiString
      } yield m + ((e.keys.head, v))
    )
  } yield (e.keys.head, es.last(e.keys.head), MapView((title, NonEmptyList(e, es))))
