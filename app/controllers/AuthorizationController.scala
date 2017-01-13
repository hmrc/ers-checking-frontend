/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L50

import scala.concurrent.Future
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future
import connectors.AuthenticationConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, IdentityConfidencePredicate}
import utils.ExternalUrls

// $COVERAGE-OFF$
object AuthorizationController extends AuthorizationController

trait AuthorizationController extends FrontendController
with Actions
with AuthenticationConnector {

  val pageVisibilityPredicate = new IdentityConfidencePredicate(L50, Future.successful(Forbidden))
  implicit val context: ErsContext = ErsContextImpl

  def notAuthorised = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
      Future.successful(Ok(views.html.not_authorised.render(request, context)))
  }

}
