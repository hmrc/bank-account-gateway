package uk.gov.hmrc.bankaccountgateway

import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.routing.sird.{POST => SPOST, _}
import play.api.test.Helpers._
import play.core.server.{Server, ServerConfig}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class DownstreamConnectorSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  val insightsPort = 11222

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.bank-account-insights.port" -> insightsPort)
    .build()

  private val connector = app.injector.instanceOf[DownstreamConnector]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "Checking connectivity" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "return true if the remote service returns a 200" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/check/insights") =>
            Action(Ok("{}").withHeaders("Content-Type" -> "application/json"))
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/check/insights", "1234"))
        result shouldBe true
      }
    }

    "return true if the remote service returns a 400" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/check/insights") =>
            Action(BadRequest("{}").withHeaders("Content-Type" -> "application/json"))
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/check/insights", "1234"))
        result shouldBe true
      }
    }

    "return false if the remote service returns a 401" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/check/insights") =>
            Action(Unauthorized("{}").withHeaders("Content-Type" -> "application/json"))
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/check/insights", "1234"))
        result shouldBe false
      }
    }

    "return false if the remote service returns a 404" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/check/insights") =>
            Action(NotFound)
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/check/insights", "1234"))
        result shouldBe false
      }
    }

    "return false if the remote service returns a 500" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/check/insights") =>
            Action(InternalServerError)
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/check/insights", "1234"))
        result shouldBe false
      }
    }

    "return false if the remote service returns a 502" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/check/insights") =>
            Action(BadGateway)
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/check/insights", "1234"))
        result shouldBe false
      }
    }
  }
}
