import mill._
import mill.scalajslib._
import mill.scalajslib.api._
import mill.scalalib._
import scalafmt._

trait DincyclopediaModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.4.2"

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

  val catsCoreV       = "2.10.0"
  val catsEffectV     = "3.5.2"
  val catsParseV      = "1.0.0"
  val kittensV        = "3.4.0"
  val shapelessV      = "3.3.0"
  val circeV          = "0.14.6"
  val circeTaggedAdtV = "0.11.0"
  val refinedV        = "0.11.0"
  val woofV           = "0.7.0"
  val osLibV          = "0.9.1"
  val http4sV         = "1.0.0-M41"
  val http4sDomV      = "0.2.11"
}

trait DincyclopediaJSModule extends DincyclopediaModule with ScalaJSModule {
  def scalaJSVersion = "1.16.0"
}

trait DincyclopediaTestModule extends TestModule.Munit {
  val circeV             = "0.14.6"
  val munitV             = "1.0.0"
  val munitCatsEffectV   = "2.0.0"
  val disciplineV        = "2.0.0"
  val scalacheckDerivedV = "0.5.0"

  def ivyDeps = Agg(
    ivy"org.scalameta::munit::$munitV",
    ivy"org.scalameta::munit-scalacheck::$munitV",
    ivy"org.typelevel::munit-cats-effect::$munitCatsEffectV",
    ivy"org.typelevel::discipline-munit::$disciplineV",
    ivy"io.circe::circe-testing::$circeV",
    ivy"io.github.martinhh::scalacheck-derived::$scalacheckDerivedV",
  )
}

object `package` extends RootModule with DincyclopediaModule {
  def resources = T {
    os.makeDir(T.dest / "webapp")
    val jsPath = ui.fastLinkJS().dest.path
    os.copy(jsPath / "main.js", T.dest / "webapp" / "main.js")
    os.copy(jsPath / "main.js.map", T.dest / "webapp" / "main.js.map")
    super.resources() ++ Seq(PathRef(T.dest))
  }

  object model extends Module {
    trait Shared extends DincyclopediaModule with PlatformScalaModule {
      def ivyDeps = Agg(
        ivy"org.typelevel::cats-core::$catsCoreV",
        ivy"org.typelevel::kittens::$kittensV",
        ivy"io.circe::circe-core::$circeV",
        ivy"io.circe::circe-generic::$circeV",
        ivy"eu.timepit::refined::$refinedV",
        ivy"org.legogroup::woof-core::$woofV",
      )
    }

    object jvm extends Shared {
      object test extends ScalaTests with DincyclopediaTestModule
    }

    object js extends Shared with DincyclopediaJSModule {
      object test extends ScalaJSTests with DincyclopediaTestModule
    }
  }

  object parser extends DincyclopediaModule {
    def mainClass = Some("dincyclopedia.parser.JsonParser")

    def moduleDeps = Seq(model.jvm)

    def ivyDeps = Agg(
      ivy"org.typelevel::cats-core:$catsCoreV",
      ivy"org.typelevel::cats-effect:$catsEffectV",
      ivy"org.typelevel::cats-parse:$catsParseV",
      ivy"com.lihaoyi::os-lib:$osLibV",
      ivy"eu.timepit::refined:$refinedV",
      ivy"org.legogroup::woof-core:$woofV",
    )

    object test extends ScalaTests with DincyclopediaTestModule
  }

  object ui extends DincyclopediaJSModule {
    def scalaJSDomV = "2.8.0"
    def calicoV     = "0.2.2"

    def moduleDeps = Seq(model.js)

    def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-dom::$scalaJSDomV",
      ivy"com.armanbilge::calico::$calicoV",
      ivy"io.circe::circe-core::$circeV",
      ivy"io.circe::circe-generic::$circeV",
      ivy"io.circe::circe-literal::$circeV",
      ivy"io.circe::circe-parser::$circeV",
      ivy"org.http4s::http4s-client::$http4sV",
      ivy"org.http4s::http4s-dom::$http4sDomV",
    )

    def moduleKind = T(ModuleKind.CommonJSModule)

    object test extends ScalaJSTests with DincyclopediaTestModule
  }
}
