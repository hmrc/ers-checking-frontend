import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val silencerVersion = "1.7.1"
  val akkaVersion = "2.6.14"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.3.0",
    "uk.gov.hmrc" %% "domain" % "5.11.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-27",
    "uk.gov.hmrc" %% "tabular-data-validator" % "1.4.0",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.24",
    "commons-io" % "commons-io" % "2.9.0",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "2.0.2",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.14",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,

    "uk.gov.hmrc" %% "play-frontend-hmrc" % "0.68.0-play-27"

  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.9",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3",
    "org.mockito" % "mockito-core" % "3.10.0",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.jsoup" % "jsoup" % "1.13.1",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
