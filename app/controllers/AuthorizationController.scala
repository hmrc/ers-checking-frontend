/*
 * Copyright 2020 HM Revenue & Customs
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

import config.{ErsContext, ErsContextImpl}
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.auth.IdentityConfidencePredicate
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L50
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future
// $COVERAGE-OFF$
object AuthorizationController extends AuthorizationController

trait AuthorizationController extends FrontendController {

  val messages = applicationMessages
  val authAction: AuthAction = AuthAction

  implicit val context: ErsContext = ErsContextImpl

  def notAuthorised: Action[AnyContent] = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.not_authorised.render(request, context, messages)))
  }

}
