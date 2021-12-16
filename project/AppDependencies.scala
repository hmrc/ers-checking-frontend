import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val silencerVersion = "1.7.7"
  val akkaVersion = "2.6.17"
  val bootstrapVersion = "5.18.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik"             % "silencer-lib"                % silencerVersion % Provided cross CrossVersion.full,
    "commons-io"                  % "commons-io"                  % "2.11.0",
    "com.lightbend.akka"          %% "akka-stream-alpakka-csv"    % "3.0.4",
    "com.typesafe.akka"           %% "akka-stream"                % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                 % akkaVersion,
    "com.typesafe.akka"           %% "akka-protobuf"              % akkaVersion,
    "com.typesafe.akka"           %% "akka-actor-typed"           % akkaVersion,
    "com.typesafe.akka"           %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka"           %% "akka-http-spray-json"       % "10.2.7",
    "net.sourceforge.htmlcleaner" % "htmlcleaner"                 % "2.25",
    "uk.gov.hmrc"                 %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"                 %% "domain"                     % "6.2.0-play-28",
    "uk.gov.hmrc"                 %% "http-caching-client"        % "9.5.0-play-28",
    "uk.gov.hmrc"                 %% "tabular-data-validator"     % "1.4.0",
    "uk.gov.hmrc"                 %% "play-frontend-hmrc"         % "1.31.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "com.vladsch.flexmark"    % "flexmark-all"             % "0.62.2",
    "com.github.tomakehurst" % "wiremock-standalone"     % "2.27.2",
    "com.typesafe.akka"      %% "akka-testkit"           % akkaVersion,
    "org.jsoup"              % "jsoup"                   % "1.14.3",
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.10.0",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion
  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
