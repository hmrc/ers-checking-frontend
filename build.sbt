import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "ers-checking-frontend"

lazy val scoverageSettings: Seq[Def.Setting[_]] =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;prod.*;app.*;.*BuildInfo.*;view.*;.*Connector.*;.*Metrics;.*config;.*Global;.*Routes;.*RoutesPrefix;.*Configuration;config.AuditFilter;config.LoggingFilter;.*config.WSHttp;models.*;controllers.ERSCheckingBaseController;services.AllWSHttp;",
    ScoverageKeys.coverageMinimum := 86,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(publishingSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(PlayKeys.playDefaultPort := 9225)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    routesImport += "models.upscan.UploadId"
  )
  .settings(majorVersion := 4)

scalacOptions ++= Seq(
  "-P:silencer:pathFilters=views;routes"
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
