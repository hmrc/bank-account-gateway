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

package uk.gov.hmrc.bankaccountgateway.controllers

import play.api.mvc._
import uk.gov.hmrc.bankaccountgateway.DownstreamConnector
import uk.gov.hmrc.bankaccountgateway.config.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton()
class BankAccountReputationController @Inject()(cc: ControllerComponents, config: AppConfig, connector: DownstreamConnector)
  extends BackendController(cc) {

  def any(): Action[AnyContent] = Action.async { implicit request =>
    val path = request.target.uri.toString.replace("bank-account-gateway", "bank-account-reputation")
    val url = s"${config.barsBaseUrl}$path"

    connector.forward(request, url)
  }
}
