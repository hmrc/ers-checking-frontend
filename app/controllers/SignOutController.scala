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
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SignOutController @Inject()(val mcc: MessagesControllerComponents,
                                  signedOutView: views.html.signed_out)
                                 (implicit val ec: ExecutionContext,
                                  val appConfig: ApplicationConfig) extends FrontendController(mcc) with I18nSupport with Logging{


  def timedOut(): Action[AnyContent] = Action { implicit request =>
        logger.info(s"[SignOutContoller][timeout] user remained inactive on the service, user has been signed out")
        Ok(signedOutView(request,request2Messages,appConfig)).withNewSession
  }

  def onSubmit(): Action[AnyContent] = Action { _ =>
    Redirect(
      appConfig.signIn,
      Map("continue_url" -> Seq(appConfig.loginCallback),
          "origin" -> Seq(appConfig.appName))
    )
  }
}