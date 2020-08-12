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

import play.api.Play
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.i18n.Lang
import play.api.mvc.Call
import controllers.routes

import scala.concurrent.duration._

trait ApplicationConfig {
  val assetsPrefix: String
  val analyticsToken: Option[String]
  val analyticsHost: String
	val upscanProtocol: String
	val upscanInitiateHost: String
	val upscanRedirectBase: String
	val odsSuccessRetryAmount: Int
	val odsValidationRetryAmount: Int
	val allCsvFilesCacheRetryAmount: Int
	val retryDelay: FiniteDuration
  val startElement: String
  val endElement: String
  val ggSignInUrl: String
  val languageTranslationEnabled: Boolean
  val reportAProblemNonJSUrl: String
  val reportAProblemPartialUrl: String
  def languageMap: Map[String, Lang]
  def routeToSwitchLanguage: String => Call
}

class ApplicationConfigImpl extends ApplicationConfig with ServicesConfig {
  protected def mode: play.api.Mode.Mode = Play.current.mode
  protected def runModeConfiguration: play.api.Configuration = Play.current.configuration
  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private lazy val contactHost = baseUrl("contact-frontend")
  private val contactFormServiceIdentifier = "ERS-CHECKING"

	override lazy val upscanProtocol: String = getConfString("upscan.protocol","http").toLowerCase()
	override lazy val upscanInitiateHost: String = baseUrl("upscan")
	override lazy val upscanRedirectBase: String = getString("microservice.services.upscan.redirect-base")

	override lazy val odsSuccessRetryAmount: Int = runModeConfiguration.getInt("retry.ods-success-cache.complete-upload.amount").getOrElse(3)
	override lazy val odsValidationRetryAmount: Int = runModeConfiguration.getInt("retry.ods-success-cache.validation.amount").getOrElse(3)
	override lazy val allCsvFilesCacheRetryAmount: Int = runModeConfiguration.getInt("retry.csv-success-cache.all-files-complete.amount").getOrElse(3)
	override lazy val retryDelay: FiniteDuration = runModeConfiguration.getMilliseconds("retry.delay").get milliseconds

	override lazy val assetsPrefix: String = loadConfig(s"govuk-tax.assets.url") + loadConfig(s"govuk-tax.assets.version")
  override lazy val analyticsToken: Option[String] = runModeConfiguration.getString("govuk-tax.google-analytics.token")
  override lazy val analyticsHost: String = runModeConfiguration.getString("govuk-tax.google-analytics.host").getOrElse("service.gov.uk")

  override lazy val startElement: String = "<table:table-row"
  override lazy val endElement: String = "</table:table-row>"

  override val ggSignInUrl: String = runModeConfiguration.getString("govuk-tax.government-gateway-sign-in.host").getOrElse("")

  override lazy val languageTranslationEnabled: Boolean = runModeConfiguration.getBoolean("microservice.services.features.welsh-translation").getOrElse(true)

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
}

object ApplicationConfig extends ApplicationConfigImpl
