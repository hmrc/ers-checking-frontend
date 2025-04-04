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

package controllers

import config.ApplicationConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class AuthorisationController @Inject()(mcc: MessagesControllerComponents,
                                        implicit val appConfig: ApplicationConfig,
                                        not_authorised: views.html.not_authorised,
                                        individual_not_authorised: views.html.individual_not_authorised,
                                        individual_signout: views.html.individual_signout
                                       ) extends FrontendController(mcc) with I18nSupport {
  def notAuthorised: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Unauthorized(not_authorised(request, request2Messages, appConfig)))
  }

  def individualNotAuthorised: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Unauthorized(individual_not_authorised(request, request2Messages, appConfig)))
  }

  def individualSignout: Action[AnyContent] = Action { implicit request =>
    Ok(individual_signout(request, request2Messages, appConfig))
  }

  def individualSignoutRedirect: Action[AnyContent] = Action { _ =>
    Redirect(routes.AuthorisationController.individualSignout()).withNewSession
  }
}