/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.{HttpEntity, MimeTypes}
import play.api.libs.json.JsValue
import play.api.mvc.Results.{BadGateway, InternalServerError, MethodNotAllowed}
import play.api.mvc.{AnyContent, Request, ResponseHeader, Result}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownstreamConnector @Inject()(httpClient: HttpClient) {
  private val logger = Logger(this.getClass.getSimpleName)

  def forward(request: Request[AnyContent], url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

    logger.info(s"Forwarding to downstream url: $url")

    (request.method, request.headers(CONTENT_TYPE).toLowerCase()) match {
      case ("POST", "application/json") =>
        val onwardHeaders = request.headers.remove(CONTENT_LENGTH).headers

        try {
          httpClient.POST[Option[JsValue], HttpResponse](url = url, body = request.body.asJson, onwardHeaders)
            .map { response: HttpResponse =>
              val returnHeaders = response.headers
                .filterNot { case (n, _) => n == CONTENT_TYPE || n == CONTENT_LENGTH }
                .mapValues(x => x.mkString)

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
