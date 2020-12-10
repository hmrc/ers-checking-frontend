import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "ers-checking-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "2.1.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.60.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.16.0-play-26",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "play-partials" % "7.0.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.1.0-play-26",
    "uk.gov.hmrc" %% "play-language" % "4.5.0-play-26",
    "uk.gov.hmrc" %% "auth-client" % "3.2.0-play-26",
    "uk.gov.hmrc" %% "tabular-data-validator" % "1.0.0",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.22",
    "commons-io" % "commons-io" % "2.6"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
        "org.scalatest" %% "scalatest" % "3.0.9" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
        "org.mockito" % "mockito-core" % "3.4.6" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.9.2" % scope,
				"com.github.tomakehurst" % "wiremock-standalone" % "2.27.2" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
