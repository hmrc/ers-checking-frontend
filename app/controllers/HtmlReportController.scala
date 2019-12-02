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

import play.Logger
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{CacheUtil, ContentUtil, HtmlCreator, JsonParser}
import models.SheetErrors
import uk.gov.hmrc.services.validation.ValidationError
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object HtmlReportController extends HtmlReportController {
  override val cacheUtil: CacheUtil = CacheUtil
}

trait HtmlReportController extends ERSCheckingBaseController {

  var jsonParser = JsonParser
  val cacheUtil: CacheUtil

  def htmlErrorReportPage() = AuthenticatedBy(ERSGovernmentGateway, pageVisibilityPredicate).async {
    implicit authContext =>
      implicit request =>
        showHtmlErrorReportPage()
  }

  def showHtmlErrorReportPage()(implicit authContext: AuthContext, request: Request[AnyRef], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    cacheUtil.fetchAll().map { all =>
      val scheme: (String, String) = all.getEntry[String](CacheUtil.SCHEME_CACHE) match {
        case Some(name) => ContentUtil.getSchemeName(name)(messages)
        case _ => ("", "")
      }
      val schemeName = scheme._1
      val schemeErrors: ListBuffer[SheetErrors] = all.getEntry[ListBuffer[SheetErrors]](CacheUtil.ERROR_LIST_CACHE).getOrElse(new ListBuffer[SheetErrors]())
      val schemeErrorCount: Long = all.getEntry[Long](CacheUtil.SCHEME_ERROR_COUNT_CACHE).getOrElse(0)
      val schemeNameShort = scheme._2
      var totalErrors = 0
      for (sheet <- schemeErrors) {
        val sheetErrors: ListBuffer[ValidationError] = sheet.errors
        for (errors <- sheetErrors) {
            totalErrors += 1
        }
      }
      val sheets: String = HtmlCreator.getSheets(schemeErrors)(messages)
      Ok(views.html.html_error_report(schemeName, sheets, schemeNameShort, totalErrors, schemeErrorCount)(request, context, messages))
    }recover {
      case e: NoSuchElementException => {
        Logger.error("Unable to display error report in HtmlReportController.showHtmlErrorReportPage. Error: " + e.getMessage, e)
        getGlobalErrorPage()(request, messages)
      }
    }
  }
  def getGlobalErrorPage()(implicit request: Request[_], messages: Messages) = Ok(views.html.global_error(messages("ers.global_errors.title"), messages("ers.global_errors.heading"), messages("ers.global_errors.message"))(request, messages))
}
