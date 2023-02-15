import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val silencerVersion = "1.7.12"
  val akkaVersion = "2.7.0"
  val bootstrapVersion = "7.13.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik"             %  "silencer-lib"               % silencerVersion % Provided cross CrossVersion.full,
    "commons-io"                  %  "commons-io"                 % "2.11.0",
    "com.lightbend.akka"          %% "akka-stream-alpakka-csv"    % "5.0.0",
    "com.typesafe.akka"           %% "akka-stream"                % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                 % akkaVersion,
    "com.typesafe.akka"           %% "akka-protobuf"              % akkaVersion,
    "com.typesafe.akka"           %% "akka-actor-typed"           % akkaVersion,
    "com.typesafe.akka"           %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka"           %% "akka-http-spray-json"       % "10.4.0",
    "net.sourceforge.htmlcleaner" %  "htmlcleaner"                % "2.26",
    "uk.gov.hmrc"                 %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"                 %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"                 %% "http-caching-client"        % "10.0.0-play-28",
    "uk.gov.hmrc"                 %% "tabular-data-validator"     % "1.5.0",
    "uk.gov.hmrc"                 %% "play-frontend-hmrc"         % "6.4.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" %  "wiremock-standalone"    % "2.27.2",
    "com.typesafe.akka"      %% "akka-testkit"           % akkaVersion,
    "com.vladsch.flexmark"   %  "flexmark-all"           % "0.62.2",
    "org.jsoup"              %  "jsoup"                  % "1.15.3",
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.10.0",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion
  ).map(_ % Test)


  val all: Seq[ModuleID] = compile ++ test
}
