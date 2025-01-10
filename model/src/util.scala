package dincyclopedia.model

import cats.Functor
import cats.Show
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import org.legogroup.woof.*
import org.legogroup.woof.Logger.withLogContext
import org.legogroup.woof.given

def debugTrace[A: Show](a: A)(using Logger[IO]): IO[A] =
  Logger[IO].debug(a.show) *> a.pure

extension [L: Show, R](e: Either[L, R]) {
  def logLeft(leftValue: R)(using Logger[IO]): IO[R] = {
    e match {
      case Left(left)   => Logger[IO].error(left.show).as(leftValue)
      case Right(right) => right.pure[IO]
    }
  }

  def logLeftToOption(using Logger[IO]): OptionT[IO, R] = {
    e match {
      case Left(left)   => OptionT(Logger[IO].error(left.show) as None)
      case Right(right) => OptionT.some(right)
    }
  }
}

extension (s: String) {
  def uncapitalize = s"${s.head.toLower}${s.tail}"
}

extension [A, B](fs: NonEmptyList[A => B]) {
  def combineWith(combine: (B, B) => B)(a: A): B =
    fs.map(_(a)).reduceLeft(combine)
}

extension [F[_], A](fa: F[Option[A]]) {
  def optionT: OptionT[F, A] = OptionT(fa)
}

extension [F[_]: Functor, A](fa: List[OptionT[F, A]]) {
  def unNone = fa.filter(_.isDefined == IO.pure(true))
}

extension [F[_]: Logger, A](fa: F[A]) {
  def withContext(cs: Contextual*): F[A] =
    fa.withLogContext(cs.map(_.pair)*)
}

extension [F[_]: Logger, A](optT: OptionT[F, A]) {
  def withContext(cs: Contextual*): OptionT[F, A] =
    optT.value.withContext(cs*).optionT
}

sealed trait Contextual(k: String, v: String) {
  def pair: (String, String) = (k, v)
}

case class Keyword(v: String)      extends Contextual("keyword", v)
case class Value(v: String)        extends Contextual("value", v)
case class Title(v: String)        extends Contextual("title", v)
case class LeveledTitle(v: String) extends Contextual("leveled title", v)
