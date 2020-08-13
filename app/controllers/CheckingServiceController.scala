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
import models.upscan.{NotStarted, UpscanCsvFilesList}
import models.{CS_checkFileType, CS_schemeType, CSformMappings}
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import services.{SessionService, UpscanService}
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

object CheckingServiceController extends CheckingServiceController {
  override val cacheUtil: CacheUtil = CacheUtil
  override val authAction: AuthAction = AuthAction
	override val upscanService: UpscanService = UpscanService

}

trait CheckingServiceController extends ERSCheckingBaseController {

  val jsonParser = JsonParser
  val uploadedFileUtil = UploadedFileUtil
  val contentUtil = ContentUtil
  val cacheUtil: CacheUtil
  val authAction: AuthAction
	val upscanService: UpscanService


	def startPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showStartPage()
  }

  def showStartPage()(implicit request: Request[AnyRef], messages: Messages): Future[Result] = Future.successful(Ok(views.html.start(request, context, messages)))

  def schemeTypePage(form: Form[CS_schemeType] = CSformMappings.schemeTypeForm): Action[AnyContent] = authAction.async {
      implicit request =>
        showSchemeTypePage(form: Form[CS_schemeType])(request, hc)
  }

  def showSchemeTypePage(form: Form[CS_schemeType])(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    Future(Ok(views.html.scheme_type(form)))
  }

  def schemeTypeSelected(): Action[AnyContent] = authAction.async {
      implicit request =>
        showSchemeTypeSelected(request)
  }

  def showSchemeTypeSelected(implicit request: Request[AnyContent]): Future[Result] = {
    CSformMappings.schemeTypeForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.scheme_type(formWithErrors)))
      },
      formData => {
        cacheUtil.cache[String](CacheUtil.SCHEME_CACHE, formData.getSchemeType).map { res =>
          Redirect(routes.CheckingServiceController.checkFileTypePage)
        }.recover {
          case e: Exception =>
            Logger.error("showSchemeTypeSelected: Unable to save scheme. Error: " + e.getMessage)
            getGlobalErrorPage
        }
      }
    )
  }


  def checkFileTypePage(form: Form[CS_checkFileType] = CSformMappings.checkFileTypeForm): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckFileTypePage(form: Form[CS_checkFileType])(request, hc)
  }

  def showCheckFileTypePage(form: Form[CS_checkFileType])(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
      Future.successful(Ok(views.html.check_file_type(form)))
  }

  def checkFileTypeSelected(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckFileTypeSelected(request)
  }

  def showCheckFileTypeSelected(implicit request: Request[AnyContent]): Future[Result] = {
    CSformMappings.checkFileTypeForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.check_file_type(formWithErrors)))
      },
      formData => {
        cacheUtil.cache[String](CacheUtil.FILE_TYPE_CACHE, formData.getFileType).map { res =>
          if (formData.getFileType == PageBuilder.OPTION_ODS) {
            Redirect(routes.CheckingServiceController.checkODSFilePage())
          } else {
            Redirect(routes.CheckCsvFilesController.selectCsvFilesPage())
          }
        }.recover {
          case e: Exception =>
						Logger.error("showCheckFileTypeSelected: Unable to save file type. Error: " + e.getMessage)
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
			scheme <- cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE)
			csvFilesList    <- cacheUtil.fetch[UpscanCsvFilesList](CacheUtil.CSV_FILES_UPLOAD, hc.sessionId.get.value)
			currentCsvFile  = csvFilesList.ids.find(ids => ids.uploadStatus == NotStarted)
			if currentCsvFile.isDefined
			upscanResponse <- upscanService.getUpscanFormData(isCSV = true, scheme, currentCsvFile)
		} yield {
			//TODO REMOVE INVALID CHARACTERS
			val invalidChars: String = "[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]"
			Ok(views.html.check_csv_file(scheme, invalidChars, currentCsvFile.get.fileId)(request, request.flash, context, messages, upscanResponse))
		}) recover {
			case e: Exception =>
				Logger.error("[CheckingServiceController][showCheckCSVFilePage]: Unable to fetch scheme. Error: " + e.getMessage)
				getGlobalErrorPage()(request, messages)
		}
	}


  def checkODSFilePage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckODSFilePage()
  }

  def showCheckODSFilePage()(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
		(for {
			scheme <- cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE)
			upscanResponse <- upscanService.getUpscanFormData(isCSV = false, scheme)
			_ <- SessionService.createCallbackRecord
		} yield {
			//TODO REMOVE INVALID CHARACTERS
			val invalidChars: String = "[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]"
			Ok(views.html.check_file(scheme, invalidChars)(request, request.flash, context, messages, upscanResponse))
		}) recover {
      case e: Exception =>
				Logger.error("showCheckFilePage: Unable to fetch scheme. Error: " + e.getMessage)
				getGlobalErrorPage()(request, messages)
		}
  }

  def checkingSuccessPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckingSuccessPage()
  }

  def showCheckingSuccessPage()(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    Future.successful(Ok(views.html.checking_success.render(request, context, messages)))
  }

  def formatErrorsPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showFormatErrorsPage(request, hc)
  }

  def showFormatErrorsPage(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {

    val future = for {
      fileType <- cacheUtil.fetch[String](CacheUtil.FILE_TYPE_CACHE)
      schemeName <- cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE)
      extendedInstructions <- cacheUtil.fetch[Boolean](CacheUtil.FORMAT_ERROR_EXTENDED_CACHE)
      errorMsg <- cacheUtil.fetch[String](CacheUtil.FORMAT_ERROR_CACHE)
      errorParams <- cacheUtil.fetch[Seq[String]](CacheUtil.FORMAT_ERROR_CACHE_PARAMS)
    } yield {
      Ok(views.html.format_errors(fileType, ContentUtil.getSchemeName(schemeName)._1, ContentUtil.getSchemeName(schemeName)._2, errorMsg, errorParams, extendedInstructions))
    }

    future recover {
      case e: Exception => {
        Logger.error("showFormatErrorsPage: Unable to fetch file type. Error: " + e.getMessage)
        getGlobalErrorPage
      }
    }
  }

  def getGlobalErrorPage()(implicit request: Request[_], messages: Messages) = Ok(views.html.global_error(messages("ers.global_errors.title"), messages("ers.global_errors.heading"), messages("ers.global_errors.message"))(request, messages))

}
