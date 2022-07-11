package uk.gov.hmrc.bankaccountgateway

import play.api.http.{HttpEntity, MimeTypes}
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, Request, ResponseHeader, Result}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import play.api.http.HeaderNames._
import play.api.mvc.Results.{BadRequest, InternalServerError, MethodNotAllowed}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownstreamConnector @Inject()(httpClient: HttpClient) {
  def forward(request: Request[AnyContent], url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

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
            Future.successful(BadRequest("{\"code\": \"REQUEST_DOWNSTREAM\", \"desc\": \"An issue occurred when the downstream service tried to handle the request\"}").as(MimeTypes.JSON))
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
