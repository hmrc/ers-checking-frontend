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
import models.upscan.{NotStarted, UploadStatus, UpscanCsvFilesList}
import models.{CS_checkFileType, CS_schemeType, CSformMappings}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import repository.ErsCheckingFrontendSessionCacheRepository
import services.UpscanService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckingServiceController @Inject()(authAction: AuthAction,
                                          upscanService: UpscanService,
                                          sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                          mcc: MessagesControllerComponents,
                                          format_errors: views.html.format_errors,
                                          start: views.html.start,
                                          scheme_type: views.html.scheme_type,
                                          check_file_type: views.html.check_file_type,
                                          check_csv_file: views.html.check_csv_file,
                                          check_file: views.html.check_file,
                                          checking_success: views.html.checking_success,
                                          invalid_file: views.html.file_upload_problem,
                                          file_upload_error: views.html.file_upload_error,
                                          auditEvents: AuditEvents,
                                          override val global_error: views.html.global_error
                                         )(implicit ec: ExecutionContext, ersUtil: ERSUtil, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with ErsBaseController with Logging {

  def startPage(): Action[AnyContent] = authAction.async { implicit request =>
    showStartPage()
  }

  def showStartPage()(implicit request: Request[AnyRef], messages: Messages): Future[Result] = {
    Future.successful(Ok(start(request, messages, appConfig)))
  }

  def schemeTypePage(form: Form[CS_schemeType] = CSformMappings.schemeTypeForm): Action[AnyContent] = authAction.async {
    implicit request =>
      showSchemeTypePage(form: Form[CS_schemeType])
  }

  def showSchemeTypePage(form: Form[CS_schemeType])(implicit request: Request[AnyRef]): Future[Result] = {
    Future(Ok(scheme_type(form)))
  }

  def schemeTypeSelected(): Action[AnyContent] = authAction.async {
    implicit request =>
      showSchemeTypeSelected(request)
  }

  def showSchemeTypeSelected(implicit request: Request[AnyContent]): Future[Result] = {
    CSformMappings.schemeTypeForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(scheme_type(formWithErrors)))
      },
      formData => {
        sessionCacheService.cache[String](ersUtil.SCHEME_CACHE, formData.getSchemeType).map { _ =>
          Redirect(routes.CheckingServiceController.checkFileTypePage())
        }.recover {
          case e: Exception =>
            logger.error("[CheckingServiceController][showSchemeTypeSelected] Unable to save scheme. Error: " + e.getMessage)
            getGlobalErrorPage
        }
      }
    )
  }

  def checkFileTypePage(form: Form[CS_checkFileType] = CSformMappings.checkFileTypeForm): Action[AnyContent] = authAction.async {
    implicit request =>
      showCheckFileTypePage(form: Form[CS_checkFileType])
  }

  def showCheckFileTypePage(form: Form[CS_checkFileType])(implicit request: Request[AnyRef]): Future[Result] = {
    Future.successful(Ok(check_file_type(form)))
  }

  def checkFileTypeSelected(): Action[AnyContent] = authAction.async {
    implicit request =>
      showCheckFileTypeSelected(request)
  }

  def showCheckFileTypeSelected(implicit request: Request[AnyContent]): Future[Result] = {
    CSformMappings.checkFileTypeForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(check_file_type(formWithErrors)))
      },
      formData => {
        sessionCacheService.cache[String](ersUtil.FILE_TYPE_CACHE, formData.getFileType).map { _ =>
          if (formData.getFileType == ersUtil.OPTION_ODS) {
            Redirect(routes.CheckingServiceController.checkODSFilePage())
          } else {
            Redirect(routes.CheckCsvFilesController.selectCsvFilesPage())
          }
        }.recover {
          case e: Exception =>
            logger.error("[CheckingServiceController][showCheckFileTypeSelected] Unable to save file type. Error: " + e.getMessage)
            getGlobalErrorPage
        }
      }
    )
  }

  def checkCSVFilePage(): Action[AnyContent] = authAction.async {
    implicit request =>
      showCheckCSVFilePage()
  }

  def showCheckCSVFilePage()(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    (for {
      scheme <- sessionCacheService.fetchAndGetEntry[String](ersUtil.SCHEME_CACHE)
      csvFilesList <- sessionCacheService.fetchAndGetEntry[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD)
      currentCsvFile = csvFilesList.ids.find(ids => ids.uploadStatus == NotStarted)
      if currentCsvFile.isDefined
        upscanResponse <- upscanService.getUpscanFormData(isCSV = true, scheme, currentCsvFile)
    } yield {
      Ok(check_csv_file(scheme, currentCsvFile.get.fileId)(request, messages, upscanResponse, appConfig, ersUtil))
    }) recover {
      case _: NoSuchElementException =>
        logger.warn("[CheckingServiceController][showCheckCSVFilePage]: No files match status NotStarted")
        Redirect(routes.CheckingServiceController.fileUploadError())
      case e: Exception =>
        logger.error("[CheckingServiceController][showCheckCSVFilePage]: Unable to fetch scheme. Error: " + e.getMessage)
        getGlobalErrorPage(request, messages)
    }
  }

  def checkODSFilePage(): Action[AnyContent] = authAction.async {
    implicit request =>
      showCheckODSFilePage()
  }

  def showCheckODSFilePage()(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    (for {
      scheme <- sessionCacheService.fetchAndGetEntry[String](ersUtil.SCHEME_CACHE)
      upscanResponse <- upscanService.getUpscanFormData(isCSV = false, scheme)
      _ <- sessionCacheService.cache[UploadStatus](ersUtil.CALLBACK_DATA_KEY, NotStarted)
    } yield {
      Ok(check_file(scheme)(request, messages, upscanResponse, appConfig))
    }) recover {
      case e: Exception =>
        logger.error("[CheckingServiceController][showCheckODSFilePage] Unable to fetch scheme. Error: " + e.getMessage)
        getGlobalErrorPage(request, messages)
    }
  }

  def checkingSuccessPage(): Action[AnyContent] = authAction.async {
    implicit request =>
      showCheckingSuccessPage()
  }

  def showCheckingSuccessPage()(implicit request: Request[AnyRef], messages: Messages): Future[Result] = {
    Future.successful(Ok(checking_success(request, messages, appConfig)))
  }

  def checkingInvalidFilePage(): Action[AnyContent] = authAction.async {
    implicit request =>
      showInvalidFilePage()
  }

  def showInvalidFilePage()(implicit request: Request[AnyRef], messages: Messages): Future[Result] = {
      Future.successful(BadRequest(invalid_file("ers.file_problem.title")(request, messages, appConfig)))
  }

  def formatErrorsPage(): Action[AnyContent] = authAction.async {
    implicit request =>
      showFormatErrorsPage()
  }

  def showFormatErrorsPage()(implicit request: Request[AnyRef]): Future[Result] = {
    val future = for {
      fileType <- sessionCacheService.fetchAndGetEntry[String](ersUtil.FILE_TYPE_CACHE)
      schemeName <- sessionCacheService.fetchAndGetEntry[String](ersUtil.SCHEME_CACHE)
      extendedInstructions <- sessionCacheService.fetchAndGetEntry[Boolean](ersUtil.FORMAT_ERROR_EXTENDED_CACHE)
      errorMsg <- sessionCacheService.fetchAndGetEntry[String](ersUtil.FORMAT_ERROR_CACHE)
      errorParams <- sessionCacheService.fetchAndGetEntry[Seq[String]](ersUtil.FORMAT_ERROR_CACHE_PARAMS)
    } yield {
      auditEvents.fileProcessingErrorAudit(fileType, schemeName, errorMsg)
      Ok(format_errors(
        fileType,
        ersUtil.getSchemeName(schemeName)._1,
        ersUtil.getSchemeName(schemeName)._2,
        errorMsg,
        errorParams,
        extendedInstructions))
    }

    future recover {
      case e: Exception =>
        logger.error("[CheckingServiceController][showFormatErrorsPage] Unable to fetch file type. Error: " + e.getMessage)
        getGlobalErrorPage
    }
  }

  def fileUploadError(): Action[AnyContent] = authAction {
    implicit request =>
      showFileUploadErrorPage()
  }

  def showFileUploadErrorPage()(implicit request: Request[AnyRef], messages: Messages): Result = {
    BadRequest(file_upload_error()(request, messages ,appConfig))
  }
}
