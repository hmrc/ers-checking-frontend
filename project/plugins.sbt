credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

resolvers ++= Seq(
  Resolver.url("sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
  Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
)

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.0.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "0.8.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "3.2.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.0.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.6.0")
