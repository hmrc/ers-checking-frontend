import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val silencerVersion = "1.7.12"
  val akkaVersion = "2.7.0"
  val bootstrapVersion = "7.21.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "commons-io"                  %  "commons-io"                 % "2.12.0",
    "com.lightbend.akka"          %% "akka-stream-alpakka-csv"    % "3.0.4",
    "com.typesafe.akka"           %% "akka-stream"                % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                 % akkaVersion,
    "com.typesafe.akka"           %% "akka-protobuf"              % akkaVersion,
    "com.typesafe.akka"           %% "akka-actor-typed"           % akkaVersion,
    "com.typesafe.akka"           %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka"           %% "akka-http-spray-json"       % "10.2.10",
    "net.sourceforge.htmlcleaner" %  "htmlcleaner"                % "2.28",
    "uk.gov.hmrc"                 %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"                 %% "domain"                     % "8.3.0-play-28",
    "uk.gov.hmrc"                 %% "http-caching-client"        % "10.0.0-play-28",
    "uk.gov.hmrc"                 %% "tabular-data-validator"     % "1.7.0",
    "uk.gov.hmrc"                 %% "play-frontend-hmrc"         % "7.16.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" %  "wiremock-standalone"    % "2.27.2",
    "com.typesafe.akka"      %% "akka-testkit"           % akkaVersion,
    "com.vladsch.flexmark"   %  "flexmark-all"           % "0.64.6",
    "org.jsoup"              %  "jsoup"                  % "1.16.1",
    "org.scalatestplus"      %% "mockito-4-11"           % "3.2.16.0",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion,
    "org.scalatest"          %% "scalatest"              % "3.2.16"
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
