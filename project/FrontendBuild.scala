import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse
  val appName = "ers-checking-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "1.1.0"
  private val frontendBootstrapVersion = "6.4.0"
  private val govukTemplateVersion =  "4.0.0"
  private val playUiVersion = "4.10.0"
  private val playAuthFrontendVersion = "4.7.0"
  private val playConfigVersion = "2.0.1"
  private val metricsPlayVersion = "0.2.1"
  private val metricsGraphiteVersion = "3.0.2"
  private val httpCachingVersion = "5.3.0"
  private val httpHerbsVersion = "3.3.0"
  private val playPartialVersion = "4.2.0"
  private val jsonLoggerVersion = "2.1.1"
 // private val urlBuilderVersion =  "1.0.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-partials" % playPartialVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "http-verbs" % httpHerbsVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthFrontendVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingVersion,
    "com.kenshoo" %% "metrics-play" % metricsPlayVersion,
    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,
    "uk.gov.hmrc" %% "play-json-logger" % jsonLoggerVersion,
    "uk.gov.hmrc" %% "play-breadcrumb" % "0.1.1",
    //"uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.9",
    "commons-io" % "commons-io" % "2.5",
  "uk.gov.hmrc" %% "tabular-data-validator" % "1.0.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  private val scalatestPlusPlayVersion = "1.2.0"

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "1.7.0" % scope,
        "org.scalatest" %% "scalatest" % "2.2.2" % scope,
        "org.scalatestplus" %% "play" % scalatestPlusPlayVersion % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
      //  "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}