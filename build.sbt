import uk.gov.hmrc.DefaultBuildSettings.targetJvm

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / majorVersion := 5
ThisBuild / targetJvm := "jvm-21"

lazy val microservice = Project("ers-checking-frontend", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(CodeCoverageSettings())
  .settings(PlayKeys.playDefaultPort := 9225)
  .settings(
    libraryDependencies ++= AppDependencies(),
    routesGenerator := InjectedRoutesGenerator,
    routesImport += "models.upscan.UploadId",
    scalacOptions ++= Seq(
      "-Wconf:msg=unused import&src=.html:s",
      "-Wconf:msg=unused import&src=.routes:s",
      "-Wconf:msg=unused private member&src=.routes:s",
      "-Wconf:msg=unused pattern variable&src=.routes:s"
    ),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
