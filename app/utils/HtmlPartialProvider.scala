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

package utils

import config.{ApplicationConfig, WSHttp}
import play.api.http.HeaderNames
import play.api.mvc.{Headers, Request, RequestHeader}
import play.api.{Logger, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.partials.FormPartialRetriever

trait HtmlPartialProvider extends FormPartialRetriever {
  val httpGet: WSHttp = WSHttp
  val applicationConfig = ApplicationConfig
  def crypto: String => String = new SessionCookieCryptoFilter(new ApplicationCrypto(Play.current.configuration.underlying)).encrypt

  def getHelpForm(url: String)(implicit request: Request[_]): Html = {
    val requestHeader = {
      if (!applicationConfig.languageTranslationEnabled) {
        updateCookie
      } else {
        request
      }
    }
    getPartialContent(url)(requestHeader)
  }

  def updateCookie(implicit request: Request[_]): RequestHeader = {
    val headers: Map[String, Seq[String]] = request.headers.toMap
    if(headers.contains(HeaderNames.COOKIE)) {
      val cookies = request.headers.toMap(HeaderNames.COOKIE).head
      val updatedCookies = if (cookies contains "PLAY_LANG=cy") {
        Logger.debug("[HtmlPartialProvider][updateCookie] Overriding cookie to use PLAY_LANG=en")
        cookies.replaceAllLiterally("PLAY_LANG=cy", "PLAY_LANG=en")
      } else {
        request.cookies.toString()
      }
      val updatedHeader: Headers = request.headers.replace(("Cookie", updatedCookies))
      request.copy(headers = updatedHeader)
    } else {
      Logger.debug("[HtmlPartialProvider][updateCookie] called without cookies being set")
      request
    }
  }
}

object HtmlPartialProvider extends HtmlPartialProvider