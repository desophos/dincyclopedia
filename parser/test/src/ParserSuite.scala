package dincyclopedia.parser
package test

import cats.implicits.*
import cats.parse.Parser
import weaver.*

object ParserSuite extends FunSuite {
  def expectParsed[A, B <: A](in: String, out: B)(using
      p: Parser[A]
  ): Expectations =
    matches(p.parseAll(in)) {
      case Right(a) => expect.same(a, out)
      case Left(e)  => failure(e.show)
    }

  def expectParsed[Unit](in: String)(using p: Parser[Unit]): Expectations =
    expectParsed(in, ())

  val entryData = List(
    (
      """
      Test
      {
        Key		Value

        //	CommentedKey	CommentedValue
        Key2	Value2

        /*  */
      }
      """.stripIndent.stripLeading,
      (("Test", None), Map(("Key", "Value"), ("Key2", "Value2"))),
    )
  )

  test("linebreaks") {
    given Parser[Unit] = linebreaks
    expectParsed("\n\n")
    && expectParsed("\r\n\r\n")
  }

  test("blank line") {
    given Parser[Unit] = blankLine
    expectParsed("\n")
    && expectParsed("\r\n")
    && expectParsed("\t\n")
    && expectParsed("// test \n")
    && expectParsed("\t// test \n")
    && expectParsed("\t/* \n\ttest */\n")
  }

  test("one line comment") {
    given Parser[Unit] = commentOneLine
    expectParsed("// test ")
  }

  test("multi line comment") {
    given Parser[Unit] = commentMultiLine
    expectParsed("/* test */")
    && expectParsed("/* \n\ttest \n */")
  }

  test("title line") {
    given Parser[(String, Option[(Relationship, String)])] = titleLine
    expectParsed("Test\n", ("Test", None))
    && expectParsed(
      "Test overrides Base\n",
      ("Test", Some((Relationship.overrides, "Base"))),
    )
    && expectParsed(
      "Test addsTo Base\n",
      ("Test", Some((Relationship.addsTo, "Base"))),
    )
  }

  test("keyword line") {
    given Parser[(String, String)] = keywordLine
    expectParsed("\tKey\t\tValue\n", ("Key", "Value"))
    && expectParsed("\tKey\t\tValue\t// comment \n", ("Key", "Value"))
    && expectParsed(
      "\tKey\t\t\"Quoted Value\"\t// comment \n",
      ("Key", "Quoted Value"),
    )
  }

  test("entry") {
    given Parser[((String, Option[(Relationship, String)]), Map[String, String])] =
      entry
    forEach(entryData)(expectParsed.tupled)
  }
}
