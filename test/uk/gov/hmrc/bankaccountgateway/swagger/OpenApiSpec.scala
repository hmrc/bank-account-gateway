/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bankaccountgateway.swagger

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import org.openapi4j.schema.validator.ValidationData
import org.openapi4j.schema.validator.v3.SchemaValidator
import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.MimeTypes

import java.net.URL
import scala.jdk.StreamConverters._

class OpenApiSpec extends AnyWordSpec {

  val openApiPath: URL = getClass.getResource("/api/conf/1.0/application.yaml")

  val parseOptions = new ParseOptions()
  parseOptions.setResolve(true)
  parseOptions.setResolveFully(true)

  val mapper = new ObjectMapper()
  mapper.setSerializationInclusion(Include.NON_NULL)

  "should parse" in {
    val result = new OpenAPIV3Parser().readLocation(openApiPath.toString, null, parseOptions)
    result.getMessages.size() shouldBe 0 withClue result.getMessages
  }

  "should contain valid examples" when {

    val openApi = new OpenAPIV3Parser().read(openApiPath.toString, null, parseOptions)

    openApi.getPaths.forEach { case (path, p) =>
      val verbs = Option(p.getGet).map("GET" -> _) ++ Option(p.getPost).map("POST" -> _)
      verbs.foreach { case (verb, r) =>
        val request         = r.getRequestBody.getContent.get(MimeTypes.JSON)
        val requestExamples = getExamples(request)

        s"$path $verb request examples" in {
          val json      = mapper.writeValueAsString(request.getSchema)
          val validator = new SchemaValidator(null, mapper.readTree(json))

          assume(requestExamples.nonEmpty) withClue "No examples were found for this request"
          requestExamples.foreach { e =>
            val vd = new ValidationData()
            validator.validate(e.asInstanceOf[JsonNode], vd)

            vd.isValid shouldBe true withClue vd.results()
          }
        }

        val responses = getResponses(r)
        responses.collect { case (statusCode, Some(media)) =>
          s"$path $verb $statusCode response examples" in {
            val json      = mapper.writeValueAsString(media.getSchema)
            val validator = new SchemaValidator(null, mapper.readTree(json))

            val examples = getExamples(media)
            assume(requestExamples.nonEmpty) withClue "No examples were found for this response"

            examples.foreach { e =>
              val vd = new ValidationData()
              validator.validate(e.asInstanceOf[JsonNode], vd)

              vd.isValid shouldBe true withClue vd.results()
            }
          }
        }
      }
    }
  }

  private def getResponses(r: Operation) =
    r.getResponses
      .entrySet()
      .stream()
      .map { e =>
        e.getKey -> Option(e.getValue.getContent).map(_.get(MimeTypes.JSON))
      }
      .toScala(Map)

  private def getExamples(request: MediaType) = {
    val requestExample  = Option(request.getExample)
    val requestExamples =
      Option(request.getExamples).map(_.values().stream().toScala(Seq)).getOrElse(Seq()).map(_.getValue)
    val schemaExample   = Option(request.getSchema.getExample)
    val schemaExamples  = Option(request.getSchema.getExamples).map(_.stream().toScala(Seq)).getOrElse(Seq())

    requestExample ++ requestExamples ++ schemaExample ++ schemaExamples
  }
}
