package dincyclopedia.model

import cats.effect.IO
import org.legogroup.woof.*

val consoleOutput: Output[IO] = new Output[IO] {
  def output(str: String)      = IO.println(str)
  def outputError(str: String) = output(str)
}

given Filter  = Filter.atLeastLevel(LogLevel.Warn)
given Printer = ColorPrinter()

val makeConsoleLogger: IO[DefaultLogger[IO]] =
  DefaultLogger.makeIo(consoleOutput)
