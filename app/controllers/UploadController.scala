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

import services.{CsvFileProcessor, ProcessODSService}

import models.ERSFileProcessingException
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.CacheUtil
import play.api.mvc.{AnyContent, Request,Result}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future

object UploadController extends UploadController {
	override val processODSService: ProcessODSService = ProcessODSService
	override val cacheUtil: CacheUtil = CacheUtil
	override val csvFileProcessor: CsvFileProcessor = CsvFileProcessor
}

trait UploadController extends ERSCheckingBaseController {

	val processODSService: ProcessODSService
	val cacheUtil: CacheUtil
	val csvFileProcessor: CsvFileProcessor

	def uploadCSVFile(scheme: String) = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
		implicit authContext =>
			implicit request =>
				showuploadCSVFile(scheme)
	}

	def showuploadCSVFile(scheme: String)(implicit authContext: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
				try {
					val result = csvFileProcessor.processCsvUpload(scheme)(request,authContext, hc)
					result match {
						case true => Future(Redirect(routes.CheckingServiceController.checkingSuccessPage))
						case false => Future(Redirect(routes.HtmlReportController.htmlErrorReportPage))
					}
				} catch {
					case e:  ERSFileProcessingException => cacheUtil.cache[String](CacheUtil.FORMAT_ERROR_CACHE, e.message).flatMap { res =>
						cacheUtil.cache[Boolean](CacheUtil.FORMAT_ERROR_EXTENDED_CACHE, e.needsExtendedInstructions).map { r =>
							Redirect(routes.CheckingServiceController.formatErrorsPage())
						}
					}
				}
	}

	def uploadODSFile(scheme: String) = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
		implicit authContext =>
			implicit request =>
				showuploadODSFile(scheme)
	}

	def showuploadODSFile(scheme: String)(implicit authContext: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
		try {
			val result = processODSService.performODSUpload()(request, scheme, authContext, hc,applicationMessages)
			result match {
				case true => Future(Redirect(routes.CheckingServiceController.checkingSuccessPage))
				case false => Future(Redirect(routes.HtmlReportController.htmlErrorReportPage))
			}
		} catch {
			case e: ERSFileProcessingException =>
				cacheUtil.cache[String](CacheUtil.FORMAT_ERROR_CACHE, e.message).flatMap { res =>
					cacheUtil.cache[Boolean](CacheUtil.FORMAT_ERROR_EXTENDED_CACHE, e.needsExtendedInstructions).map { r =>
						Redirect(routes.CheckingServiceController.formatErrorsPage())
					}
				}
		}
	}



}
