import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val akkaVersion = "2.6.21" //Current 'bobby rule' not to upgrade past 2.6.21
  val bootstrapVersion = "7.23.0"
  val mongoVersion = "1.4.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "commons-io"                  %  "commons-io"                 % "2.14.0",
    "com.lightbend.akka"          %% "akka-stream-alpakka-csv"    % "4.0.0",
    "com.typesafe.akka"           %% "akka-stream"                % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                 % akkaVersion,
    "com.typesafe.akka"           %% "akka-protobuf"              % akkaVersion,
    "com.typesafe.akka"           %% "akka-actor-typed"           % akkaVersion,
    "com.typesafe.akka"           %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka"           %% "akka-http-spray-json"       % "10.2.10",
    "net.sourceforge.htmlcleaner" %  "htmlcleaner"                % "2.29",
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-play-28"         % mongoVersion,
    "uk.gov.hmrc"                 %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"                 %% "domain"                     % "8.3.0-play-28",
    "uk.gov.hmrc"                 %% "tabular-data-validator"     % "1.8.0",
    "uk.gov.hmrc"                 %% "play-frontend-hmrc"         % "7.22.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "org.wiremock"           %  "wiremock-standalone"    % "3.3.1",
    "com.typesafe.akka"      %% "akka-testkit"           % akkaVersion,
    "com.vladsch.flexmark"   %  "flexmark-all"           % "0.64.8",
    "org.jsoup"              %  "jsoup"                  % "1.16.2",
    "org.scalatestplus"      %% "mockito-4-11"           % "3.2.17.0",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % mongoVersion,
    "org.scalatest"          %% "scalatest"              % "3.2.17"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
