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

import play.Logger
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{CacheUtil, ContentUtil, HtmlCreator, JsonParser}
import models.SheetErrors
import uk.gov.hmrc.services.validation.ValidationError

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object HtmlReportController extends HtmlReportController {
  override val cacheUtil: CacheUtil = CacheUtil
}

trait HtmlReportController extends ERSCheckingBaseController {

  var jsonParser = JsonParser
  val cacheUtil: CacheUtil

  def htmlErrorReportPage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showHtmlErrorReportPage(authContext, request, hc)
  }

  def showHtmlErrorReportPage(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    cacheUtil.fetchAll().map { all =>
      val schemeName = ContentUtil.getSchemeName(all.getEntry[String](CacheUtil.SCHEME_CACHE).get)._1
      val schemeErrors: ListBuffer[SheetErrors] = all.getEntry[ListBuffer[SheetErrors]](CacheUtil.ERROR_LIST_CACHE).get
      val schemeErrorCount: Long = all.getEntry[Long](CacheUtil.SCHEME_ERROR_COUNT_CACHE).get
      val schemeNameShort = ContentUtil.getSchemeName(all.getEntry[String](CacheUtil.SCHEME_CACHE).get)._2
      var totalErrors = 0
      for (sheet <- schemeErrors) {
        val sheetErrors: ListBuffer[ValidationError] = sheet.errors
        for (errors <- sheetErrors) {
            totalErrors += 1
        }
      }
      val sheets: String = HtmlCreator.getSheets(schemeErrors)
      Ok(views.html.html_error_report(schemeName, sheets, schemeNameShort, totalErrors, schemeErrorCount)(request, context))
    }recover {
      case e: NoSuchElementException => {
        Logger.error("Unable to display error report in HtmlReportController.showHtmlErrorReportPage. Error: " + e.getMessage)
        getGlobalErrorPage
      }
    }
  }

  def getGlobalErrorPage = Ok(views.html.global_error(Messages("ers.global_errors.title"), Messages("ers.global_errors.heading"), Messages("ers.global_errors.message")))

}
