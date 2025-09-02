import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val pekkoVersion = "1.0.3"
  val bootstrapVersion = "9.19.0"
  val mongoVersion = "2.7.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "commons-io"                  %  "commons-io"                   % "2.20.0",
    "org.apache.pekko"            %% "pekko-stream"                 % pekkoVersion,
    "org.apache.pekko"            %% "pekko-connectors-csv"         % "1.0.2",
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-play-30"           % mongoVersion,
    "uk.gov.hmrc"                 %% "bootstrap-frontend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc"                 %% "domain-play-30"               % "11.0.0",
    "uk.gov.hmrc"                 %% "tabular-data-validator"       % "1.9.0",
    "uk.gov.hmrc"                 %% "play-frontend-hmrc-play-30"   % "12.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.apache.pekko"       %% "pekko-testkit"            % pekkoVersion,
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVersion,
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
