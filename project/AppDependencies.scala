import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val silencerVersion = "1.7.1"
  val akkaVersion = "2.6.17"
  val bootstrapVersion = "5.16.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain" % "6.2.0-play-28",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-28",
    "uk.gov.hmrc" %% "tabular-data-validator" % "1.4.0",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.25",
    "commons-io" % "commons-io" % "2.11.0",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.3",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "1.22.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0",
    "org.jsoup" % "jsoup" % "1.14.3",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
