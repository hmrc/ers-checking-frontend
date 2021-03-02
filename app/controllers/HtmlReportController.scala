/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.auth.AuthAction
import javax.inject.{Inject, Singleton}
import models.SheetErrors
import models.upscan.{UploadId, UpscanCsvFilesCallbackList}
import play.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.services.validation.ValidationError
import utils.{ERSUtil, HtmlCreator, JsonParser}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HtmlReportController @Inject()(authAction: AuthAction,
                                     mcc: MessagesControllerComponents,
                                     htmlCreator: HtmlCreator,
                                     implicit val ersUtil: ERSUtil,
                                     implicit val appConfig: ApplicationConfig
                                    )(implicit executionContext: ExecutionContext) extends FrontendController(mcc) with JsonParser with I18nSupport {

  def htmlErrorReportPage(isCsv: Boolean): Action[AnyContent] = authAction.async {
      implicit request =>
        showHtmlErrorReportPage(isCsv)
  }

  def csvExtractErrors(ids: Seq[UploadId], all: CacheMap): (ListBuffer[SheetErrors], Long, Int) = {
    var totalErrors = 0
    val listBufferAndCount: Seq[(ListBuffer[SheetErrors], Long)] = ids map { id =>
      val errors = all.getEntry[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}${id.value}").getOrElse(ListBuffer())
      val errorCount = all.getEntry[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}${id.value}").getOrElse(0L)

      for (sheet <- errors) {
        val sheetErrors: ListBuffer[ValidationError] = sheet.errors
        for (_ <- sheetErrors) {
          totalErrors += 1
        }
      }

      (errors, errorCount)
    }

    val (errorsList, errorCountLong) = listBufferAndCount.reduceLeft ((accum, error) => (accum._1 ++ error._1, accum._2 + error._2))

    (errorsList, errorCountLong, totalErrors)
  }


  def showHtmlErrorReportPage(isCsv: Boolean)(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    ersUtil.fetchAll().map { all =>
      val scheme: (String, String) = all.getEntry[String](ersUtil.SCHEME_CACHE) match {
        case Some(name) => ersUtil.getSchemeName(name)
        case _ => ("", "")
      }

      lazy val (errorsList, errorCountLong, totalErrorsCount) =	if(isCsv) {
        val uploadIds: Seq[UploadId] = all.getEntry[UpscanCsvFilesCallbackList]("callback_data_key_csv").get.files.map(_.uploadId)
        csvExtractErrors(uploadIds, all)
      } else {
        val schemeErrors: ListBuffer[SheetErrors] = all.getEntry[ListBuffer[SheetErrors]](ersUtil.ERROR_LIST_CACHE).getOrElse(new ListBuffer[SheetErrors]())
        val schemeErrorCount: Long = all.getEntry[Long](ersUtil.SCHEME_ERROR_COUNT_CACHE).getOrElse(0)
        val odsErrors = schemeErrors.flatMap(sheet => sheet.errors).length
        (schemeErrors, schemeErrorCount, odsErrors)
      }

      val schemeName = scheme._1
      val schemeNameShort = scheme._2

      val sheets: String = htmlCreator.getSheets(errorsList)(messages)
      Ok(views.html.html_error_report(schemeName, sheets, schemeNameShort, totalErrorsCount, errorCountLong)(request, messages, appConfig, ersUtil))
    } recover {
      case e: NoSuchElementException =>
        Logger.error("Unable to display error report in HtmlReportController.showHtmlErrorReportPage. Error: " + e.getMessage, e)
        getGlobalErrorPage()(request, messages)
    }
  }

  def getGlobalErrorPage()(implicit request: Request[_], messages: Messages): Result = {
    Ok(views.html.global_error(
      "ers.global_errors.title",
      "ers.global_errors.message"
    )(request, messages, appConfig))
  }

}
