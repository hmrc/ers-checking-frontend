import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "2.3.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.61.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.21.0-play-26",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "play-partials" % "7.1.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.2.0-play-26",
    "uk.gov.hmrc" %% "play-language" % "4.7.0-play-26",
    "uk.gov.hmrc" %% "auth-client" % "3.2.0-play-26",
    "uk.gov.hmrc" %% "tabular-data-validator" % "0.1.0",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.22",
    "commons-io" % "commons-io" % "2.6"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26",
    "org.scalatest" %% "scalatest" % "3.0.9",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3",
    "org.mockito" % "mockito-core" % "3.4.6",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.jsoup" % "jsoup" % "1.9.2",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2",
    "com.typesafe.play" %% "play-test" % PlayVersion.current
  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
