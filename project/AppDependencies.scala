import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "8.1.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % "test",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "test"
  )

  val itTest = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % "it",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "it"
  )
}
