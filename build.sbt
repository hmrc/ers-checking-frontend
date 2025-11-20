ThisBuild / scalaVersion := "2.13.16"
ThisBuild / majorVersion := 4

lazy val microservice = Project("ers-checking-frontend", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(CodeCoverageSettings())
  .settings(PlayKeys.playDefaultPort := 9225)
  // TODO: REMOVE, THIS IS JUST TO PUBLISH/READ the file-validator locally
  .settings(
    resolvers += Resolver.defaultLocal
  )
  .settings(
    libraryDependencies ++= AppDependencies(),
    routesGenerator := InjectedRoutesGenerator,
    routesImport += "models.upscan.UploadId",
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s", "-Wconf:cat=unused-imports&src=html/.*:s"
    ),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
