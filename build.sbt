import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / scalaVersion                     := "2.13.16"
ThisBuild / majorVersion                     := 0

val appName = "bank-account-gateway"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
  )
  .settings(PlayKeys.playDefaultPort := 8345)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings*)

lazy val it = project.in(file("it"))
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
