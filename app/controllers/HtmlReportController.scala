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

import controllers.auth.AuthAction
import play.Logger
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{CacheUtil, ContentUtil, HtmlCreator, JsonParser}
import models.SheetErrors
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallbackList}
import uk.gov.hmrc.services.validation.ValidationError
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

object HtmlReportController extends HtmlReportController {
  override val cacheUtil: CacheUtil = CacheUtil
  override val authAction: AuthAction = AuthAction
}

trait HtmlReportController extends ERSCheckingBaseController {

  var jsonParser = JsonParser
  val cacheUtil: CacheUtil
  val authAction: AuthAction

  def htmlErrorReportPage(isCsv: Boolean): Action[AnyContent] = authAction.async {
      implicit request =>
        showHtmlErrorReportPage(isCsv)
  }

	def csvExtractErrors(ids: Seq[UploadId], all: CacheMap): (ListBuffer[SheetErrors], Long, Int) = {
		var totalErrors = 0
		val listBufferAndCount: Seq[(ListBuffer[SheetErrors], Long)] = ids map { id =>
			val errors = all.getEntry[ListBuffer[SheetErrors]](s"${CacheUtil.ERROR_LIST_CACHE}$id").getOrElse(ListBuffer())
			val errorCount = all.getEntry[Long](s"${CacheUtil.SCHEME_ERROR_COUNT_CACHE}$id").getOrElse(0L)

			for (sheet <- errors) {
				val sheetErrors: ListBuffer[ValidationError] = sheet.errors
				for (errors <- sheetErrors) {
					totalErrors += 1
				}
			}

			(errors, errorCount)
		}

		val (errorsList, errorCountLong) = listBufferAndCount.reduceLeft ((accum, error) => (accum._1 ++ error._1, accum._2 + error._2))

		(errorsList, errorCountLong, totalErrors)
	}


  def showHtmlErrorReportPage(isCsv: Boolean)(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    cacheUtil.fetchAll().map { all =>
      val scheme: (String, String) = all.getEntry[String](CacheUtil.SCHEME_CACHE) match {
        case Some(name) => ContentUtil.getSchemeName(name)(messages)
        case _ => ("", "")
      }


			lazy val (errorsList, errorCountLong, totalErrorsCount) =	if(isCsv) {
				val uploadIds: Seq[UploadId] = all.getEntry[UpscanCsvFilesCallbackList]("callback_data_key_csv").get.files.map(_.uploadId)
				csvExtractErrors(uploadIds, all)
			} else {
				val schemeErrors: ListBuffer[SheetErrors] = all.getEntry[ListBuffer[SheetErrors]](CacheUtil.ERROR_LIST_CACHE).getOrElse(new ListBuffer[SheetErrors]())
				val schemeErrorCount: Long = all.getEntry[Long](CacheUtil.SCHEME_ERROR_COUNT_CACHE).getOrElse(0)
				val odsErrors = schemeErrors.flatMap(sheet => sheet.errors).length
				(schemeErrors, schemeErrorCount, odsErrors)
			}

      val schemeName = scheme._1
      val schemeNameShort = scheme._2

      val sheets: String = HtmlCreator.getSheets(errorsList)(messages)
      Ok(views.html.html_error_report(schemeName, sheets, schemeNameShort, totalErrorsCount, errorCountLong)(request, context, messages))
    }recover {
      case e: NoSuchElementException =>
				Logger.error("Unable to display error report in HtmlReportController.showHtmlErrorReportPage. Error: " + e.getMessage, e)
				getGlobalErrorPage()(request, messages)
		}
  }

  def getGlobalErrorPage()(implicit request: Request[_], messages: Messages): Result = {
		Ok(views.html.global_error(
			messages("ers.global_errors.title"),
			messages("ers.global_errors.heading"),
			messages("ers.global_errors.message")
		)(request, messages))
	}

}
