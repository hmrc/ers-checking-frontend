import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "ers-checking-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._


  private val frontendBootstrapVersion = "8.19.0"
  private val httpCachingVersion = "7.0.0"
  private val playPartialVersion = "6.1.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-partials" % playPartialVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingVersion,
    "uk.gov.hmrc" %% "play-breadcrumb" % "1.0.0",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.21",
    "commons-io" % "commons-io" % "2.5",
    "uk.gov.hmrc" %% "tabular-data-validator" % "1.0.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  private val scalatestVersion = "2.2.6"
  private val scalatestPlusPlayVersion = "1.5.1"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.10.3"
  private val hmrcTestVersion = "2.3.0"
  private val mockitoVersion = "2.8.47"


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
