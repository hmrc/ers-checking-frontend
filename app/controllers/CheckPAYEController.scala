/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalEmpRefAndPAYE}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class CheckPAYEController @Inject()(authAction: AuthAction,
                                    mcc: MessagesControllerComponents,
                                    not_enrolled_in_paye: views.html.not_enrolled_in_paye,
                                    sign_out_paye: views.html.sign_out_paye,
                                    override val global_error: views.html.global_error
                                   )(val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with ErsBaseController with Logging {

  def missingPAYE(): Action[AnyContent] = authAction.async {
    implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent] =>
      showMissingPAYE()
  }

  def showMissingPAYE()(implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent], messages: Messages): Future[Result] = {
    Future.successful(Ok(not_enrolled_in_paye(request, messages, appConfig)))
  }

  def signOutPAYE(): Action[AnyContent] = authAction.async {
    implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent] =>
      showSignOutPAYE()
  }

  def showSignOutPAYE()(implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent], messages: Messages): Future[Result] = {
    Future.successful(Ok(sign_out_paye(request, messages, appConfig)).withNewSession)
  }
}
