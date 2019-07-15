/*
 * Copyright 2019 HM Revenue & Customs
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

import models.CSformMappings
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object CheckingServiceController extends CheckingServiceController {
  override val cacheUtil: CacheUtil = CacheUtil
}

trait CheckingServiceController extends ERSCheckingBaseController {

  val jsonParser = JsonParser
  val uploadedFileUtil = UploadedFileUtil
  val contentUtil = ContentUtil
  val cacheUtil: CacheUtil

  def startPage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showStartPage()
  }

  def showStartPage()(implicit authContext: AuthContext, request: Request[AnyRef], messages: Messages): Future[Result] = Future.successful(Ok(views.html.start(request, context, messages)))

  def schemeTypePage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showSchemeTypePage(authContext, request, hc)
  }

  def showSchemeTypePage(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    val schemeType: String = "0"
    Future(Ok(views.html.scheme_type(schemeType)))
  }

  def schemeTypeSelected() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showSchemeTypeSelected(authContext, request)
  }

  def showSchemeTypeSelected(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
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


  def checkFileTypePage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showCheckFileTypePage(authContext, request, hc)
  }

  def showCheckFileTypePage(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    cacheUtil.fetch[String](CacheUtil.FILE_TYPE_CACHE).map { fileType =>
      Ok(views.html.check_file_type(fileType))
    } recover {
      case e: NoSuchElementException => Ok(views.html.check_file_type(""))
    }
  }

  def checkFileTypeSelected() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showCheckFileTypeSelected(authContext, request)
  }

  def showCheckFileTypeSelected(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
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

  def checkCSVFilePage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showCheckCSVFilePage()
  }

  def showCheckCSVFilePage()(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE).map { scheme =>
        val invalidChars: String = "[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]"
        Ok(views.html.check_csv_file(scheme, invalidChars)(request, request.flash, context, messages))
    } recover {
      case e: Exception => {
        Logger.error("showCheckCSVFilePage: Unable to fetch scheme. Error: " + e.getMessage)
        getGlobalErrorPage
      }
    }
  }


  def checkODSFilePage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showCheckODSFilePage()
  }

  def showCheckODSFilePage()(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE).map { scheme =>
      val invalidChars: String = "[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]"
      Ok(views.html.check_file(scheme, invalidChars)(request, request.flash, context, messages))
    } recover {
      case e: Exception => {
        Logger.error("showCheckFilePage: Unable to fetch scheme. Error: " + e.getMessage)
        getGlobalErrorPage
      }
    }
  }

  def checkingSuccessPage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showCheckingSuccessPage()
  }

  def showCheckingSuccessPage()(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    Future.successful(Ok(views.html.checking_success.render(request, context, messages)))
  }

  def checkingErrorsPage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showCheckingErrorsPage(authContext, request, hc)
  }

  def showCheckingErrorsPage(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
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

  def formatErrorsPage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showFormatErrorsPage(authContext, request, hc)
  }

  def showFormatErrorsPage(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    cacheUtil.fetch[String](CacheUtil.FILE_TYPE_CACHE).flatMap { fileType =>
      cacheUtil.fetch[String](CacheUtil.SCHEME_CACHE).flatMap { schemeName =>
        cacheUtil.fetch[Boolean](CacheUtil.FORMAT_ERROR_EXTENDED_CACHE).flatMap { extendedInstructions =>
          cacheUtil.fetch[String](CacheUtil.FORMAT_ERROR_CACHE).map { errorMsg =>
            Ok(views.html.format_errors(fileType, ContentUtil.getSchemeName(schemeName)._1, ContentUtil.getSchemeName(schemeName)._2, errorMsg, extendedInstructions))
          }
        }
      }
    } recover {
      case e: Exception => {
        Logger.error("showFormatErrorsPage: Unable to fetch file type. Error: " + e.getMessage)
        getGlobalErrorPage
      }
    }
  }

  def getGlobalErrorPage = Ok(views.html.global_error(Messages("ers.global_errors.title"), Messages("ers.global_errors.heading"), Messages("ers.global_errors.message")))

}
