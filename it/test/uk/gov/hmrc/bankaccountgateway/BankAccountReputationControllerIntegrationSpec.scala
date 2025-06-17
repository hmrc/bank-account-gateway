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

package uk.gov.hmrc.bankaccountgateway

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.http.scaladsl.model.MediaTypes
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.test.ExternalWireMockSupport

class BankAccountReputationControllerIntegrationSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneServerPerSuite
  with ExternalWireMockSupport {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port/bank-account-gateway"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false,
        "microservice.services.bank-account-reputation.port" -> externalWireMockPort,
        "microservice.rejectInternalTraffic" -> false // Disable internal traffic rejection for testing
      )
      .build()

  private val CORRELATION_ID_HEADER_NAME = "CorrelationId"

  private val testCorrelationId = "f0bd1f32-de51-45cc-9b18-0520d6e3ab1a"

  "BankAccountReputationController" should {
    "respond with OK status" when {
      "verifying a business account" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/bank-account-reputation/verify/business"))
            .withRequestBody(equalToJson("""{"sortCode":"123456", "accountNumber": "12345678"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
            .withHeader(CORRELATION_ID_HEADER_NAME, equalTo(testCorrelationId)) // ensure correlation ID is passed to the downstream service
            .willReturn(
              aResponse()
                .withBody("""{"status":"VERIFIED", "code": "Phone verification code successfully sent"}""")
                .withStatus(OK)
            )
        )

        val response =
          wsClient
            .url(s"$baseUrl/verify/business")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .withHttpHeaders(CORRELATION_ID_HEADER_NAME -> testCorrelationId)
            .post(Json.parse("""{"sortCode":"123456", "accountNumber":"12345678"}"""))
            .futureValue

        response.status shouldBe OK
        response.header(CORRELATION_ID_HEADER_NAME) shouldBe Some(testCorrelationId) // ensure correlation ID is echoed back on the response
      }

      "verifying a personal account" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/bank-account-reputation/verify/personal"))
            .withRequestBody(equalToJson("""{"sortCode":"123456", "accountNumber": "12345678"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
            .withHeader(CORRELATION_ID_HEADER_NAME, equalTo(testCorrelationId)) // ensure correlation ID is passed to the downstream service
            .willReturn(
              aResponse()
                .withBody("""{"status":"VERIFIED", "code": "Phone verification code successfully sent"}""")
                .withStatus(OK)
            )
        )

        val response =
          wsClient
            .url(s"$baseUrl/verify/personal")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .withHttpHeaders(CORRELATION_ID_HEADER_NAME -> testCorrelationId)
            .post(Json.parse("""{"sortCode":"123456", "accountNumber":"12345678"}"""))
            .futureValue

        response.status shouldBe OK
        response.header(CORRELATION_ID_HEADER_NAME) shouldBe Some(testCorrelationId) // ensure correlation ID is echoed back on the response
      }
    }


  }
}
