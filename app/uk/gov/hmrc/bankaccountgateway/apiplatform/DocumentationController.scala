/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.bankaccountgateway.apiplatform

import controllers.Assets
import play.api.Configuration
import play.api.http.{ContentTypes, MimeTypes}
import play.api.mvc.{Action, AnyContent, Codec, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import views.txt

import javax.inject.Inject
import scala.concurrent.Future

class DocumentationController @Inject()(assets: Assets, cc: ControllerComponents, configuration: Configuration) extends BackendController(cc) {
  private val apiStatus = configuration.get[String]("api-platform.status")

  def definition(): Action[AnyContent] = Action.async {
    Future.successful(Ok(txt.definition(apiStatus)).as(ContentTypes.withCharset(MimeTypes.JSON)(Codec.utf_8)))
  }

  def raml(version: String, file: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", file)
  }
}