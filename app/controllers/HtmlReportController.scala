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
import controllers.auth.AuthAction

import javax.inject.{Inject, Singleton}
import models.SheetErrors
import models.upscan.{UploadId, UpscanCsvFilesCallbackList}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Reads
import play.api.mvc._
import repository.ErsCheckingFrontendSessionCacheRepository
import services.audit.AuditEvents
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{ERSUtil, JsonParser}
import uk.gov.hmrc.services.validation.models.ValidationError

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HtmlReportController @Inject()(authAction: AuthAction,
                                     mcc: MessagesControllerComponents,
                                     sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                     html_error_report: views.html.html_error_report,
                                     auditEvents: AuditEvents,
                                     override val global_error: views.html.global_error
                                    )(implicit executionContext: ExecutionContext, ersUtil: ERSUtil, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with JsonParser with I18nSupport with ErsBaseController with Logging {

  def htmlErrorReportPage(isCsv: Boolean): Action[AnyContent] = authAction.async {
    implicit request =>
      showHtmlErrorReportPage(isCsv)
  }

  def csvExtractErrors(ids: Seq[UploadId], all: CacheItem): (ListBuffer[SheetErrors], Long, Int) = {
    var totalErrors = 0
    val listBufferAndCount: Seq[(ListBuffer[SheetErrors], Long)] = ids map { id =>
      val errors = getEntry[ListBuffer[SheetErrors]](all, s"${ersUtil.ERROR_LIST_CACHE}${id.value}").getOrElse(ListBuffer())
      val errorCount = getEntry[Long](all, s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}${id.value}").getOrElse(0L)

      for (sheet <- errors) {
        val sheetErrors: ListBuffer[ValidationError] = sheet.errors
        for (_ <- sheetErrors) {
          totalErrors += 1
        }
      }

      (errors, errorCount)
    }

    val (errorsList, errorCountLong) = listBufferAndCount.reduceLeft((accum, error) => (accum._1 ++ error._1, accum._2 + error._2))

    (errorsList, errorCountLong, totalErrors)
  }


  def showHtmlErrorReportPage(isCsv: Boolean)(implicit request: Request[AnyRef], messages: Messages): Future[Result] = {
    sessionCacheService.fetchAll().map { all =>
      val scheme: (String, String) = getEntry[String](all, ersUtil.SCHEME_CACHE) match {
        case Some(name) => ersUtil.getSchemeName(name)
        case _ => ("", "")
      }
      lazy val (errorsList, errorCountLong, totalErrorsCount) = if (isCsv) {
        val uploadIds: Seq[UploadId] = getEntry[UpscanCsvFilesCallbackList](all, ersUtil.CALLBACK_DATA_KEY_CSV)
          .get.files.map(_.uploadId)
        csvExtractErrors(uploadIds, all)
      } else {
        val schemeErrors: ListBuffer[SheetErrors] = getEntry[ListBuffer[SheetErrors]](all, ersUtil.ERROR_LIST_CACHE)
          .getOrElse(new ListBuffer[SheetErrors]())
        val schemeErrorCount: Long = getEntry[Long](all, ersUtil.SCHEME_ERROR_COUNT_CACHE).getOrElse(0)
        val odsErrors = schemeErrors.flatMap(sheet => sheet.errors).length
        (schemeErrors, schemeErrorCount, odsErrors)
      }

      val (schemeName, schemeNameShort) = scheme

      val sheetNameList = errorsList.map(ele => ele.sheetName)
      val sheetName = sheetNameList.headOption.getOrElse("SheetName Not Found")
      val errorMsg = errorsList
        .flatMap(ele => ele.errors.map(e => e.errorMsg))
        .toSeq
        .flatMap(error => {
          if (error.contains(".")) {
            Some(error.split("\\.").last)
          } else {
            None
          }
        })
        .distinct
        .mkString(",")

      auditEvents.fileProcessingErrorAudit(schemeName, sheetName, errorMsg)
      Ok(html_error_report(schemeName, schemeNameShort, totalErrorsCount, errorCountLong, errorsList.toSeq)(request, messages, appConfig))
    } recover {
      case e: NoSuchElementException =>
        auditEvents.auditRunTimeError(e, "Failed to get values ", "")
        logger.error("Unable to display error report in HtmlReportController.showHtmlErrorReportPage. Error: " + e.getMessage, e)
        getGlobalErrorPage(request, messages)
    }
  }

  def getEntry[T](cacheItem: CacheItem, key: String)(implicit reads: Reads[T]): Option[T] =
    cacheItem.data.value
      .get(key).map(json =>
        json.validate[T].fold(
          errors => throw new InternalServerException(s"CacheItem entry for $key could not be parsed, errors: $errors"),
          valid => valid
        )
      )

}
