import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val silencerVersion = "1.7.1"

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
    "uk.gov.hmrc" %% "tabular-data-validator" % "0.1.1",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.22",
    "commons-io" % "commons-io" % "2.6",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "2.0.2",
    "com.typesafe.akka" %% "akka-stream" % "2.6.12",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.12",
    "com.typesafe.akka" %% "akka-protobuf" % "2.6.12",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.12",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full

  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26",
    "org.scalatest" %% "scalatest" % "3.0.9",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3",
    "org.mockito" % "mockito-core" % "3.4.6",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.jsoup" % "jsoup" % "1.9.2",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "com.typesafe.akka" %% "akka-testkit" % "2.6.12"

  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
