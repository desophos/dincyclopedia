package soldaktools
package test

import cats.effect.IO
import org.legogroup.woof.*

val consoleOutput: Output[IO] = new Output[IO] {
  def output(str: String)      = IO.delay(println(str))
  def outputError(str: String) = IO.delay(System.err.println(str))
}

given Filter  = Filter.everything
given Printer = ColorPrinter()
