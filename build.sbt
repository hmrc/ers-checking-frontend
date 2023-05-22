import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "ers-checking-frontend"

lazy val scoverageSettings: Seq[Def.Setting[?]] =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;prod.*;app.*;.*BuildInfo.*;view.*;.*Connector.*;.*Metrics;.*config;.*Global;.*Routes;.*RoutesPrefix;.*Configuration;config.AuditFilter;config.LoggingFilter;.*config.WSHttp;models.*;controllers.ERSCheckingBaseController;services.AllWSHttp;",
    ScoverageKeys.coverageMinimumStmtTotal := 83,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(scoverageSettings *)
  .settings(PlayKeys.playDefaultPort := 9225)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies ++= AppDependencies.all,
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    routesImport += "models.upscan.UploadId"
  )
  .settings(majorVersion := 4)

scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s", "-Wconf:cat=unused-imports&src=html/.*:s"
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

addCommandAlias("scalastyleAll", "all scalastyle test:scalastyle")
