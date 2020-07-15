import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "ers-checking-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._


  private val frontendBootstrapVersion = "12.9.0"
  private val httpCachingVersion = "8.0.0"
  private val playPartialVersion = "6.3.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-partials" % playPartialVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingVersion,
    "uk.gov.hmrc" %% "play-breadcrumb" % "1.0.0",
    "uk.gov.hmrc" %% "play-language" % "4.3.0-play-25",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.22",
    "commons-io" % "commons-io" % "2.6",
    "uk.gov.hmrc" %% "tabular-data-validator" % "1.0.0",
    "uk.gov.hmrc" %% "auth-client" % "2.35.0-play-25"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  private val scalatestVersion = "3.0.8"
  private val scalatestPlusPlayVersion = "2.0.1"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.10.3"
  private val hmrcTestVersion = "3.4.0-play-25"
  private val mockitoVersion = "2.23.4"


  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion % scope,
        "org.mockito" % "mockito-core" % mockitoVersion % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
      //  "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
