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

import controllers.auth.{AuthAction, RequestWithOptionalEmpRef}
import models._
import models.upscan.{NotStarted, UploadId, UpscanCsvFilesList, UpscanIds}
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{CacheUtil, PageBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CheckCsvFilesController extends CheckCsvFilesController {
	override val cacheUtil: CacheUtil = CacheUtil
	override val pageBuilder: PageBuilder = PageBuilder
	override val authAction: AuthAction = AuthAction
}

trait CheckCsvFilesController extends ERSCheckingBaseController {
	val cacheUtil: CacheUtil
	val pageBuilder: PageBuilder
	val authAction: AuthAction

	def selectCsvFilesPage(): Action[AnyContent] = authAction.async {
		implicit request =>
				showCheckCsvFilesPage()(request, hc)
	}

	def showCheckCsvFilesPage()(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Result] = {
		(for {
			_ 					<- cacheUtil.remove(CacheUtil.CSV_FILES_UPLOAD)
			scheme 			<- cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE)
			_ 					<- SessionService.createCallbackRecordCsv(hc.sessionId.get.value)
		} yield {
			val csvFilesList: Seq[CsvFiles] = PageBuilder.getCsvFilesList(scheme)
			Ok(views.html.select_csv_file_types(scheme, csvFilesList))
		}) recover {
			case _: Throwable => getGlobalErrorPage
		}
	}

	def checkCsvFilesPageSelected(): Action[AnyContent] = authAction.async {
			implicit request =>
				validateCsvFilesPageSelected()
	}

	def validateCsvFilesPageSelected()(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Result] = {
		CSformMappings.csvFileCheckForm.bindFromRequest.fold(
			formWithError =>
				reloadWithError(Some(formWithError)),
			formData =>
				performCsvFilesPageSelected(formData)
		)
	}

	def performCsvFilesPageSelected(formData: Seq[CsvFiles])(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
		val csvFilesCallbackList: UpscanCsvFilesList = createCacheData(formData)
		if(csvFilesCallbackList.ids.isEmpty) {
			reloadWithError()
		} else {
			(for{
				_   <- cacheUtil.cache(CacheUtil.CSV_FILES_UPLOAD, csvFilesCallbackList, hc.sessionId.get.value)
			} yield {
				Redirect(routes.CheckingServiceController.checkCSVFilePage())
			}).recover {
				case e: Throwable =>
					Logger.error(s"[CheckCsvFilesController][performCsvFilesPageSelected]: Save data to cache failed with exception ${e.getMessage}.", e)
					getGlobalErrorPage
			}
		}
	}

	def createCacheData(csvFilesList: Seq[CsvFiles]): UpscanCsvFilesList = {
		val ids = for(fileData <- csvFilesList if fileData.isSelected.contains(PageBuilder.OPTION_YES)) yield {
			UpscanIds(UploadId.generate, fileData.fileId, NotStarted)
		}
		UpscanCsvFilesList(ids)
	}

	def reloadWithError(form: Option[Form[List[CsvFiles]]] = None)(implicit messages: Messages): Future[Result] = {
		val errorKey = if(form.isDefined) form.get.errors.head.message else "no_file_error"
		Future.successful(
			Redirect(routes.CheckCsvFilesController.selectCsvFilesPage())
				.flashing("csv-file-not-selected-error" -> messages(s"${PageBuilder.PAGE_CHECK_CSV_FILE}.$errorKey"))
		)
	}

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(views.html.global_error(
			messages("ers.global_errors.title"),
			messages("ers.global_errors.heading"),
			messages("ers.global_errors.message"))(request, messages))
	}
}
