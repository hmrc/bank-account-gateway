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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, urlEqualTo}
import org.apache.pekko.http.scaladsl.model.MediaTypes
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes}
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.test.ExternalWireMockSupport

class BankAccountInsightsControllerIntegrationSpec extends AnyWordSpec
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
        "microservice.services.bank-account-insights.port" -> externalWireMockPort
      )
      .build()


  "BankAccountInsightsController" should {
    "respond with OK status" when {
      "a valid json payload is provided" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/bank-account-insights/check/insights"))
            .withRequestBody(equalToJson("""{"sortCode":"123456", "accountNumber": "12345678"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
            .willReturn(
              aResponse()
                .withBody("""{"status":"VERIFIED", "code": "Phone verification code successfully sent"}""")
                .withStatus(OK)
            )
        )

        val response =
          wsClient
            .url(s"$baseUrl/check/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post(Json.parse("""{"sortCode":"123456", "accountNumber":"12345678"}"""))
            .futureValue

        response.status shouldBe OK
      }
    }

    "respond with BAD_REQUEST status" when {
      "an invalid json payload is provided" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/bank-account-insights/check/insights"))
            .withRequestBody(equalToJson("""{"sortCode":"123456", "accountNumber": "12345678"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
            .willReturn(
              aResponse()
                .withBody("""{"correlationId":"1234567809823498457", "risk": 0, "reason": "ACCOUNT_NOT_ON_WATCH_LIST"}""")
                .withStatus(OK)
            )
        )

        val response =
          wsClient
            .url(s"$baseUrl/check/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"sortCode":"123456", "accountNumber":"12345678}""")
            .futureValue

        response.status shouldBe BAD_REQUEST
        Json.parse(response.body) shouldBe Json.parse("""{"statusCode":400,"message":"bad request, cause: invalid json"}""")
      }
    }
  }
}
