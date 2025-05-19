/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.bankaccountgateway.controllers

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.routing.sird.{POST => SPOST, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.core.server.{Server, ServerConfig}

class BankAccountReputationControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val barsPort = 11222

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.bank-account-reputation.port" -> barsPort)
    .build()

  private val controller = app.injector.instanceOf[BankAccountReputationController]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "POST /verify/personal" should {

    "forward a 200 response from the downstream service" in {
      val response = """{
                       |  "accountNumberIsWellFormatted": "yes",
                       |  "accountExists": "yes",
                       |  "nameMatches": "partial",
                       |  "accountName": "Mr J Bloggs",
                       |  "sortCodeIsPresentOnEISCD": "yes",
                       |  "sortCodeSupportsDirectDebit": "yes",
                       |  "sortCodeSupportsDirectCredit": "yes",
                       |  "nonStandardAccountDetailsRequiredForBacs": "no"
                       |}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(barsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/bank-account-reputation/verify/personal") =>
            r.headers.get("True-Calling-Client") shouldBe Some("example-service")
            r.headers.get("Authorization") shouldBe Some("1234")
            Action(Ok(response).withHeaders("Content-Type" -> "application/json"))
        }
      } { _ =>

        val fakeRequest = FakeRequest("POST", "/bank-account-gateway/verify/personal")
          .withJsonBody(Json.parse("""{"account": {"accountNumber": "12345667", "sortCode": "123456"}, "subject": {"name": "Mr J Bloggs"}}"""))
          .withHeaders("True-Calling-Client" -> "example-service", "Content-Type" -> "application/json", "Authorization" -> "abc")

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe response
      }
    }

    "forward a 400 response from the downstream service" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: Subject"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(barsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/bank-account-reputation/verify/personal") => Action(
            BadRequest(errorResponse).withHeaders("Content-Type" -> "application/json"))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/bank-account-gateway/verify/personal")
          .withJsonBody(Json.parse("""{"account": {"accountNumber": "12345667", "sortCode": "123456"}}"""))
          .withHeaders("True-Calling-Client" -> "example-service", "Content-Type" -> "application/json")

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "handle a malformed json payload" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: Subject"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(barsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/bank-account-reputation/verify/personal") => Action(
            BadRequest(errorResponse).withHeaders("Content-Type" -> "application/json"))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/bank-account-gateway/verify/personal")
          .withTextBody("""{"account": {""")
          .withHeaders("True-Calling-Client" -> "example-service", "Content-Type" -> "application/json")

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "return bad gateway if there is no connectivity to the downstream service" in {
      val errorResponse = """{"code": "REQUEST_DOWNSTREAM", "desc": "An issue occurred when the downstream service tried to handle the request"}""".stripMargin

      val fakeRequest = FakeRequest("POST", "/bank-account-gateway/verify/personal")
        .withJsonBody(Json.parse("""{"account": {"accountNumber": "12345667", "sortCode": "123456"}}"""))
        .withHeaders("True-Calling-Client" -> "example-service", "Content-Type" -> "application/json")

      val result = controller.any()(fakeRequest)
      status(result) shouldBe Status.BAD_GATEWAY
      contentAsString(result) shouldBe errorResponse
    }

  }
}
