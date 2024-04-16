import mill._
import mill.scalajslib._
import mill.scalajslib.api._
import mill.scalalib._
import scalafmt._

trait SoldakToolsModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.3.1"
  // def scalaJSVersion = "1.15.0"

  def catsCoreV       = "2.10.0"
  def catsEffectV     = "3.5.2"
  def catsParseV      = "1.0.0"
  def shapelessV      = "3.3.0"
  def circeV          = "0.14.6"
  def circeTaggedAdtV = "0.11.0"
  def refinedV        = "0.11.0"
  def woofV           = "0.7.0"
  def osLibV          = "0.9.1"

  def http4sV    = "1.0.0-M40"
  def http4sDomV = "0.2.10"

  def scalacOptions = Seq(
    "-explain",
    "-explain-types",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-no-indent",
    "-Xfatal-warnings",
    "-Xmax-inlines",
    "128",
    "-Ysafe-init",
    "-Wunused:imports",
  )

  // def moduleKind = T(ModuleKind.CommonJSModule)

  // def jsEnvConfig = T(
  //   JsEnvConfig.Selenium(JsEnvConfig.Selenium.ChromeOptions(false))
  // )
}

trait SoldakToolsTestModule extends TestModule.Weaver {
  def munitV           = "1.0.0-M10"
  def munitCatsEffectV = "2.0.0-M4"

  def ivyDeps = Agg(
    ivy"org.scalameta::munit:$munitV",
    ivy"org.scalameta::munit-scalacheck:$munitV",
    ivy"org.typelevel::munit-cats-effect:$munitCatsEffectV",
    ivy"com.disneystreaming::weaver-cats:0.8.3",
  )
}

object soldaktools extends RootModule with SoldakToolsModule {
  def moduleDeps = Seq(parser, api)

  object test extends ScalaTests with SoldakToolsTestModule {
    def moduleDeps = super.moduleDeps ++ Seq(parser.test, api.test)
  }

  object parser extends SoldakToolsModule {
    def ivyDeps = Agg(
      ivy"org.typelevel::cats-core:$catsCoreV",
      ivy"org.typelevel::cats-effect:$catsEffectV",
      ivy"org.typelevel::cats-parse:$catsParseV",
      ivy"com.lihaoyi::os-lib:$osLibV",
      ivy"eu.timepit::refined:$refinedV",
      ivy"org.legogroup::woof-core:$woofV",
    )

    object test extends ScalaTests with SoldakToolsTestModule
  }

  object api extends SoldakToolsModule {
    def ivyDeps = Agg(
      ivy"org.typelevel::cats-core:$catsCoreV",
      ivy"org.typelevel::cats-effect:$catsEffectV",
      // ivy"org.typelevel::shapeless3-deriving:$shapelessV",
      ivy"org.http4s::http4s-circe:$http4sV",
      ivy"org.http4s::http4s-dsl:$http4sV",
      ivy"org.http4s::http4s-client:$http4sV",
      ivy"org.http4s::http4s-ember-client:$http4sV",
      ivy"io.circe::circe-core:$circeV",
      ivy"io.circe::circe-generic:$circeV",
      ivy"io.circe::circe-literal:$circeV",
      ivy"io.circe::circe-parser:$circeV",
      ivy"org.latestbit::circe-tagged-adt-codec:$circeTaggedAdtV",
      ivy"eu.timepit::refined:$refinedV",
      ivy"org.legogroup::woof-core:$woofV",
    )

    object test extends ScalaTests with SoldakToolsTestModule
  }

//  object ui extends SoldakToolsModule {
//    def moduleKind = T(ModuleKind.CommonJSModule)
//
//    def scalaJSDomV = "2.4.0"
//    def calicoV     = "0.2.1"
//
//    def moduleDeps = Seq(api)
//
//    def ivyDeps = Agg(
//      ivy"org.scala-js::scalajs-dom::$scalaJSDomV",
//      ivy"com.armanbilge::calico::$calicoV",
//      ivy"org.http4s::http4s-client::$http4sV",
//      ivy"org.http4s::http4s-dom::$http4sDomV",
//    )
//  }
}

/** Update the millw script.
  */
def millw() = T.command {
  val target = mill.util.Util.download(
    "https://raw.githubusercontent.com/lefou/millw/main/millw.ps1"
  )
  val millw = build.millSourcePath / "mill"
  os.copy.over(target.path, millw)
  os.perms.set(
    millw,
    os.perms(millw) + java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
  )
  target
}
