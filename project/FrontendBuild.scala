import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse
  val appName = "ers-checking-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "2.0.0"
  private val frontendBootstrapVersion = "7.10.0"
  private val govukTemplateVersion =  "5.0.0"
  private val playUiVersion = "5.1.0"
  private val playAuthFrontendVersion = "6.2.0"
  private val playConfigVersion = "3.0.0"
  //private val metricsPlayVersion = "2.4.0_0.4.1"
  private val metricsGraphiteVersion = "3.0.2"
  private val httpCachingVersion = "6.0.0"
  private val playPartialVersion = "5.2.0"
  private val jsonLoggerVersion = "3.0.0"
 // private val urlBuilderVersion =  "1.0.0"



  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-partials" % playPartialVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthFrontendVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingVersion,
   // "com.kenshoo" %% "metrics-play" % metricsPlayVersion,
    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,
    "uk.gov.hmrc" %% "play-json-logger" % jsonLoggerVersion,
    "uk.gov.hmrc" %% "play-breadcrumb" % "0.1.1",
    //"uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.9",
    "commons-io" % "commons-io" % "2.5",
    "uk.gov.hmrc" %% "tabular-data-validator" % "1.0.0"//,
    //"io.dropwizard.metrics" %% "metrics-core" % "3.1.2"
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
        "org.mockito" % "mockito-core" % "2.2.16" % scope,
      //  "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}