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

package uk.gov.hmrc.bankaccountgateway.connectors

import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{HttpEntity, MimeTypes}
import play.api.libs.json.JsObject
import play.api.mvc.Results.{BadGateway, InternalServerError, MethodNotAllowed}
import play.api.mvc.{AnyContent, Request, ResponseHeader, Result}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownstreamConnector @Inject()(httpClient: HttpClientV2) {
  private val logger = Logger(this.getClass.getSimpleName)

  def forward(request: Request[AnyContent], url: String, authToken: String)(implicit ec: ExecutionContext): Future[Result] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

    logger.info(s"Forwarding to downstream url: $url")

    (request.method, request.headers(CONTENT_TYPE).toLowerCase()) match {
      case ("POST", "application/json") =>
        val onwardHeaders = request.headers.remove(CONTENT_LENGTH, HOST, AUTHORIZATION).headers
        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(authToken)))

        try {
          httpClient
            .post(url"$url")
            .withBody(request.body.asJson.getOrElse(JsObject.empty))
            .setHeader(onwardHeaders: _*)
            .execute[HttpResponse]
            .map { response: HttpResponse =>
              val returnHeaders = response.headers
                .filterNot { case (n, _) => n == CONTENT_TYPE || n == CONTENT_LENGTH }
                .view.mapValues(x => x.mkString).toMap

              Result(
                ResponseHeader(response.status, returnHeaders),
                HttpEntity.Streamed(response.bodyAsSource, None, response.header(CONTENT_TYPE)))
            }.recoverWith { case t: Throwable =>
            Future.successful(BadGateway("{\"code\": \"REQUEST_DOWNSTREAM\", \"desc\": \"An issue occurred when the downstream service tried to handle the request\"}").as(MimeTypes.JSON))
          }
        } catch {
          case t: Throwable =>
            Future.successful(InternalServerError("{\"code\": \"REQUEST_FORWARDING\", \"desc\": \"An issue occurred when forwarding the request to the downstream service\"}").as(MimeTypes.JSON))
        }

      case _ =>
        Future.successful(MethodNotAllowed("{\"code\": \"UNSUPPORTED_METHOD\", \"desc\": \"Unsupported HTTP method or content-type\"}").as(MimeTypes.JSON))
    }
  }
  
}
