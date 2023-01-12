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

package controllers.auth

import config.ApplicationConfig
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.domain.EmpRef

import scala.concurrent.{ExecutionContext, Future}

case class RequestWithOptionalEmpRef[A](request: Request[A], optionalEmpRef: Option[EmpRef]) extends WrappedRequest[A](request)
trait AuthIdentifierAction extends ActionBuilder[RequestWithOptionalEmpRef, AnyContent] with ActionFunction[Request, RequestWithOptionalEmpRef]

@Singleton
class AuthAction @Inject()(override val authConnector: AuthConnector,
                           appConfig: ApplicationConfig,
                           val parser: BodyParsers.Default
                          )(implicit val executionContext: ExecutionContext) extends AuthorisedFunctions with AuthIdentifierAction with Logging {

  val origin: String = "ers-checking-frontend"

  def loginParams: Map[String, Seq[String]] = Map(
    "continue_url" -> Seq(appConfig.loginCallback),
    "origin" -> Seq(origin)
  )

  override def invokeBlock[A](request: Request[A], block: RequestWithOptionalEmpRef[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

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
        logger.warn(s"[AuthAction][invokeBlock] no active session for uri: ${request.uri} with message: ${er.getMessage}", er)
        Redirect(appConfig.signIn, loginParams)
      case er: AuthorisationException =>
        logger.warn(s"[AuthAction][invokeBlock] Auth exception: ${er.getMessage} for  uri ${request.uri}")
        Redirect(controllers.routes.AuthorisationController.notAuthorised().url)
    }
  }
}
