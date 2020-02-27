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

package controllers.auth

import config.ApplicationConfig
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, Request, Result, WrappedRequest}
import playconfig.ERSAuthConnector
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisationException, AuthorisedFunctions, ConfidenceLevel, EnrolmentIdentifier, NoActiveSession}
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import utils.ExternalUrls

import scala.concurrent.{ExecutionContext, Future}

case class RequestWithOptionalEmpRef[A](request: Request[A], optionalEmpRef: Option[EmpRef]) extends WrappedRequest[A](request)

trait AuthAction extends AuthorisedFunctions with ActionBuilder[RequestWithOptionalEmpRef] {

  val authConnector: AuthConnector
  implicit val ec: ExecutionContext
  lazy val signInUrl: String = ApplicationConfig.ggSignInUrl
  val origin: String = "ers-checking-frontend"

  def loginParams: Map[String, Seq[String]] = Map(
    "continue" -> Seq(ExternalUrls.loginCallback),
    "origin" -> Seq(origin)
  )


  override def invokeBlock[A](request: Request[A], block: RequestWithOptionalEmpRef[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

    def getIdentifierValue(identifiers: Seq[EnrolmentIdentifier])(key: String): Option[String] = identifiers.collectFirst{
      case EnrolmentIdentifier(`key`, value) => value
    }

    authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L50).retrieve(allEnrolments) {
      enrolments =>
        val getIdentifier = getIdentifierValue(
          enrolments
            .getEnrolment("IR-PAYE")
            .map(_.identifiers)
            .getOrElse(Seq.empty[EnrolmentIdentifier])
        ) _

        val optionalEmpRef = (getIdentifier("TaxOfficeNumber"), getIdentifier("TaxOfficeReference")) match {
          case (Some(taxOfficeNumber), Some(taxOfficeReference)) => Some(EmpRef(taxOfficeNumber, taxOfficeReference))
          case _ => None
        }

        block(RequestWithOptionalEmpRef(request, optionalEmpRef))
    } recover {
      case er: NoActiveSession =>
        Logger.warn(s"[AuthAction][invokeBlock] no active session for uri: ${request.uri} with message: ${er.getMessage}", er)
        Redirect(ApplicationConfig.ggSignInUrl, loginParams)
      case er: AuthorisationException =>
        Logger.warn(s"[AuthAction][invokeBlock] Auth exception: ${er.getMessage} for  uri ${request.uri}")
        Redirect(controllers.routes.AuthorizationController.notAuthorised.url)
    }
  }
}

object AuthAction extends AuthAction {
  override val authConnector: AuthConnector = ERSAuthConnector
  override implicit val ec: ExecutionContext = ExecutionContext.global
}
