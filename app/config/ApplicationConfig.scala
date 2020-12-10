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

package config

import play.api.i18n.Lang
import play.api.mvc.Call
import controllers.routes
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration._

@Singleton
class ApplicationConfig @Inject()(config: ServicesConfig) {

  lazy val appName: String = config.getString("appName")
  lazy val contactHost: String = config.getString("contact-frontend.host")
  private val contactFormServiceIdentifier = "ERS-CHECKING"

  lazy val upscanProtocol: String = config.getConfString("upscan.protocol","http").toLowerCase()
  lazy val upscanInitiateHost: String = config.baseUrl("upscan")
  lazy val upscanRedirectBase: String = config.getString("microservice.services.upscan.redirect-base")

  lazy val odsSuccessRetryAmount: Int = config.getInt("retry.ods-success-cache.complete-upload.amount")
  lazy val odsValidationRetryAmount: Int = config.getInt("retry.ods-success-cache.validation.amount")
  lazy val allCsvFilesCacheRetryAmount: Int = config.getInt("retry.csv-success-cache.all-files-complete.amount")
  lazy val retryDelay: FiniteDuration = FiniteDuration(config.getString("retry.delay").toInt, "ms")

  lazy val assetsPrefix: String = config.getString("govuk-tax.assets.url") + config.getString("govuk-tax.assets.version")
  lazy val analyticsToken: String = config.getString("govuk-tax.google-analytics.token")
  lazy val analyticsHost: String = config.getString("govuk-tax.google-analytics.host")

  lazy val shortLivedCacheBaseUri: String = config.baseUrl("cachable.short-lived-cache")
  lazy val shortLivedCacheDomain: String = config.getString("microservice.services.cachable.short-lived-cache.domain")

  lazy val languageTranslationEnabled: Boolean = config.getConfBool("features.welsh-translation", defBool = true)
  def languageMap: Map[String, Lang] = Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))
  def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val googleTagManagerId: String = config.getString("google-tag-manager.id")

  lazy val chunkSize: Option[Int] = Option(config.getInt("validationChunkSize"))
  lazy val errorCount: Option[Int] = Option(config.getInt("errorDisplayCount"))

  //ExternalUrls
  lazy val basGatewayHost: String = config.getString("govuk-tax.auth.bas-gateway.host")
  lazy val loginCallback: String = Option(config.getString("govuk-tax.auth.login-callback.url")).getOrElse(routes.CheckingServiceController.startPage().url)
  lazy val loginPath: String = Option(config.getString("govuk-tax.auth.login_path")).getOrElse("sign-in")
  lazy val signIn: String = s"$basGatewayHost/bas-gateway/$loginPath"
  lazy val signOut: String = s"$basGatewayHost/bas-gateway/sign-out-without-state"
}