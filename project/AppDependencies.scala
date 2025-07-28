import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "9.18.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %%  "bootstrap-test-play-30"    % bootstrapPlayVersion,
    "io.swagger.parser.v3"          %   "swagger-parser"            % "2.1.31",
    "org.openapi4j"                 %   "openapi-schema-validator"  % "1.0.7",
    "com.fasterxml.jackson.module"  %%  "jackson-module-scala"      % "2.19.2"
  ).map(_ % Test)
}
