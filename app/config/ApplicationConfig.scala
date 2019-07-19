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

package config

import play.Logger
import play.api.Play
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.i18n.Lang
import controllers.routes

trait ApplicationConfig {
  val assetsPrefix: String
  val analyticsToken: Option[String]
  val analyticsHost: String

  val startElement: String
  val endElement: String

  val ggSignInUrl: String

  val languageTranslationEnabled: Boolean
}

class ApplicationConfigImpl extends ApplicationConfig with ServicesConfig {
  protected def mode: play.api.Mode.Mode = Play.current.mode
  protected def runModeConfiguration: play.api.Configuration = Play.current.configuration

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  Logger.info("The Getting the contact host")
  private lazy val contactHost = runModeConfiguration.getString("external-url.contact-frontend.host").getOrElse("")
  Logger.info("The contact host is " + contactHost)
  private val contactFormServiceIdentifier = "ERS-CHECKING"

  override lazy val assetsPrefix = loadConfig(s"govuk-tax.assets.url") + loadConfig(s"govuk-tax.assets.version")
  override lazy val analyticsToken: Option[String] = runModeConfiguration.getString("govuk-tax.google-analytics.token")
  override lazy val analyticsHost: String = runModeConfiguration.getString("govuk-tax.google-analytics.host").getOrElse("service.gov.uk")

  override lazy val startElement: String = "<table:table-row"
  override lazy val endElement: String = "</table:table-row>"

  override val ggSignInUrl: String = runModeConfiguration.getString("govuk-tax.government-gateway-sign-in.host").getOrElse("")

  override lazy val languageTranslationEnabled = runModeConfiguration.getBoolean("microservice.services.features.welsh-translation").getOrElse(true)

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)
}

object ApplicationConfig extends ApplicationConfigImpl
