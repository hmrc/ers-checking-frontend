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
import controllers.auth.{AuthAction, RequestWithOptionalEmpRef}

import javax.inject.{Inject, Singleton}
import models._
import models.upscan.{NotStarted, UploadId, UpscanCsvFilesList, UpscanIds}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import repository.ErsCheckingFrontendSessionCacheRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckCsvFilesController @Inject()(authAction: AuthAction,
                                        mcc: MessagesControllerComponents,
                                        sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                        select_csv_file_types: views.html.select_csv_file_types,
                                        override val global_error: views.html.global_error)
                                       (implicit ec: ExecutionContext, val ersUtil: ERSUtil, val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with ErsBaseController with Logging {

  def selectCsvFilesPage(): Action[AnyContent] = authAction.async {
    implicit request =>
        showCheckCsvFilesPage()
  }

  def showCheckCsvFilesPage()(implicit request: RequestWithOptionalEmpRef[AnyContent]): Future[Result] = {
    (for {
      _           <- sessionCacheService.delete(ersUtil.CSV_FILES_UPLOAD)
      scheme      <- sessionCacheService.fetchAndGetEntry[String](ersUtil.SCHEME_CACHE)
    } yield {
      val csvFilesList: Seq[CsvFiles] = ersUtil.getCsvFilesList(scheme)
      Ok(select_csv_file_types(scheme, csvFilesList))
    }) recover {
      case _: Throwable => getGlobalErrorPage
    }
  }

  def checkCsvFilesPageSelected(): Action[AnyContent] = authAction.async {
      implicit request =>
        validateCsvFilesPageSelected()
  }

  def validateCsvFilesPageSelected()(implicit request: RequestWithOptionalEmpRef[AnyContent]): Future[Result] = {
    CSformMappings.csvFileCheckForm().bindFromRequest().fold(
      formWithError =>
        reloadWithError(Some(formWithError)),
      formData =>
        performCsvFilesPageSelected(formData)
    )
  }

  def performCsvFilesPageSelected(formData: Seq[CsvFiles])(implicit request: Request[AnyRef]): Future[Result] = {
    val csvFilesCallbackList: UpscanCsvFilesList = createCacheData(formData)
    if(csvFilesCallbackList.ids.isEmpty) {
      reloadWithError()
    } else {
      (for{
        _   <- Future.sequence(cacheUpscanIds(csvFilesCallbackList.ids))
        _   <- sessionCacheService.cache(ersUtil.CSV_FILES_UPLOAD, csvFilesCallbackList)
      } yield {
        Redirect(routes.CheckingServiceController.checkCSVFilePage())
      }).recover {
        case e: Throwable =>
          logger.error(s"[CheckCsvFilesController][performCsvFilesPageSelected]: Save data to cache failed with exception ${e.getMessage}.", e)
          getGlobalErrorPage
      }
    }
  }

  def cacheUpscanIds(ids: Seq[UpscanIds])(implicit request: Request[AnyRef]): Seq[Future[(String, String)]] = {
    ids map { id =>
      sessionCacheService.cache[UpscanIds](id.uploadId.value, id)
    }
  }

  def createCacheData(csvFilesList: Seq[CsvFiles]): UpscanCsvFilesList = {
    val ids = for(fileData <- csvFilesList) yield {
      UpscanIds(UploadId.generate, fileData.fileId, NotStarted)
    }
    UpscanCsvFilesList(ids)
  }

  def reloadWithError(form: Option[Form[List[CsvFiles]]] = None)(implicit messages: Messages): Future[Result] = {
    val errorKey = if(form.isDefined) form.get.errors.head.message else "no_file_error"
    Future.successful(
      Redirect(routes.CheckCsvFilesController.selectCsvFilesPage())
        .flashing("csv-file-not-selected-error" -> messages(s"${ersUtil.PAGE_CHECK_CSV_FILE}.$errorKey"))
    )
  }

}
