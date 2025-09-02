import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "ers-checking-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  // To resolve dependency clash between flexmark v0.64.4+ and play-language to run accessibility tests, remove when versions align
  .settings(dependencyOverrides += "com.ibm.icu" % "icu4j" % "69.1")
  .settings(CodeCoverageSettings())
  .settings(PlayKeys.playDefaultPort := 9225)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    routesImport += "models.upscan.UploadId"
  )
  .settings(majorVersion := 4)

ThisBuild / excludeDependencies ++= Seq(
  // As of Play 3.0, groupId has changed to org.playframework; exclude transitive dependencies to the old artifacts
  // Specifically affects play-json-extensions dependency
  ExclusionRule(organization = "com.typesafe.play")
)

scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s", "-Wconf:cat=unused-imports&src=html/.*:s"
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
