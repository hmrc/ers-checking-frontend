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
import models.CSformMappings
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

object CheckingServiceController extends CheckingServiceController {
  override val cacheUtil: CacheUtil = CacheUtil
  override val authAction: AuthAction = AuthAction
}

trait CheckingServiceController extends ERSCheckingBaseController {

  val jsonParser = JsonParser
  val uploadedFileUtil = UploadedFileUtil
  val contentUtil = ContentUtil
  val cacheUtil: CacheUtil
  val authAction: AuthAction

  def startPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showStartPage()
  }

  def showStartPage()(implicit request: Request[AnyRef], messages: Messages): Future[Result] = Future.successful(Ok(views.html.start(request, context, messages)))

  def schemeTypePage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showSchemeTypePage(request, hc)
  }

  def showSchemeTypePage(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    val schemeType: String = "0"
    Future(Ok(views.html.scheme_type(schemeType)))
  }

  def schemeTypeSelected(): Action[AnyContent] = authAction.async {
      implicit request =>
        showSchemeTypeSelected(request)
  }

  def showSchemeTypeSelected(implicit request: Request[AnyRef]): Future[Result] = {
    CSformMappings.schemeTypeForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.CheckingServiceController.schemeTypePage).flashing("scheme-not-selected-error" -> Messages("ers_scheme_type.select_scheme_type")))
      },
      formData => {
        cacheUtil.cache[String](CacheUtil.SCHEME_CACHE, formData.schemeType.toString).map { res =>
          Redirect(routes.CheckingServiceController.checkFileTypePage)
        }.recover {
          case e: Exception => {
            Logger.error("showSchemeTypeSelected: Unable to save scheme. Error: " + e.getMessage)
            getGlobalErrorPage
          }
        }
      }
    )
  }


  def checkFileTypePage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckFileTypePage(request, hc)
  }

  def showCheckFileTypePage(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    cacheUtil.fetch[String](CacheUtil.FILE_TYPE_CACHE).map { fileType =>
      Ok(views.html.check_file_type(fileType))
    } recover {
      case e: NoSuchElementException => Ok(views.html.check_file_type(""))
    }
  }

  def checkFileTypeSelected(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckFileTypeSelected(request)
  }

  def showCheckFileTypeSelected(implicit request: Request[AnyRef]): Future[Result] = {
    CSformMappings.checkFileTypeForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.CheckingServiceController.checkFileTypePage).flashing("check-file-type-not-selected-error" -> Messages("ers_check_file_type.alert")))
      },
      formData => {
        cacheUtil.cache[String](CacheUtil.FILE_TYPE_CACHE, formData.checkFileType).map { res =>
          if (formData.checkFileType == PageBuilder.OPTION_ODS) {
            Redirect(routes.CheckingServiceController.checkODSFilePage)
          } else {
            Redirect(routes.CheckingServiceController.checkCSVFilePage)
          }
        }.recover {
          case e: Exception => {
            Logger.error("showCheckFileTypeSelected: Unable to save file type. Error: " + e.getMessage)
            getGlobalErrorPage
          }
        }
      }
    )
  }

  def checkCSVFilePage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckCSVFilePage()
  }

  def showCheckCSVFilePage()(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE).map { scheme =>
        val invalidChars: String = "[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]"
        Ok(views.html.check_csv_file(scheme, invalidChars)(request, request.flash, context, messages))
    } recover {
      case e: Exception => {
        Logger.error("showCheckCSVFilePage: Unable to fetch scheme. Error: " + e.getMessage)
        getGlobalErrorPage()(request, messages)
      }
    }
  }


  def checkODSFilePage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckODSFilePage()
  }

  def showCheckODSFilePage()(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE).map { scheme =>
      val invalidChars: String = "[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]"
      Ok(views.html.check_file(scheme, invalidChars)(request, request.flash, context, messages))
    } recover {
      case e: Exception => {
        Logger.error("showCheckFilePage: Unable to fetch scheme. Error: " + e.getMessage)
        getGlobalErrorPage()(request, messages)
      }
    }
  }

  def checkingSuccessPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckingSuccessPage()
  }

  def showCheckingSuccessPage()(implicit request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    Future.successful(Ok(views.html.checking_success.render(request, context, messages)))
  }

  def checkingErrorsPage(): Action[AnyContent] = authAction.async {
      implicit request =>
        showCheckingErrorsPage(request, hc)
  }

  def showCheckingErrorsPage(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    cacheUtil.fetchAll().map { all =>
        val fileType = all.getEntry[String](CacheUtil.FILE_TYPE_CACHE).get
        val schemeName = ContentUtil.getSchemeName(all.getEntry[String](CacheUtil.SCHEME_CACHE).get)._1
        val schemeNameShort = ContentUtil.getSchemeName(all.getEntry[String](CacheUtil.SCHEME_CACHE).get)._2
        val fileName = all.getEntry[String](CacheUtil.FILE_NAME_CACHE).get
          Ok(views.html.error_report(fileType, schemeName, fileName, schemeNameShort))
    } recover {
      case e: Exception => {
        Logger.error("showCheckingErrorsPage: Unable to fetch file type. Error: " + e.getMessage)
        getGlobalErrorPage
      }
    }
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
