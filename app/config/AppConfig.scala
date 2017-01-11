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

package config

import play.Logger
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val assetsPrefix: String
  val analyticsToken: Option[String]
  val analyticsHost: String

  val startElement: String
  val endElement: String

  val ggSignInUrl: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  Logger.info("The Getting the contact host")
  private lazy val contactHost = configuration.getString(s"$env.external-url.contact-frontend.host").getOrElse("")
  Logger.info("The contact host is " + contactHost)
  private val contactFormServiceIdentifier = "ERS-CHECKING"

  override lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  override lazy val assetsPrefix = loadConfig(s"govuk-tax.assets.url") + loadConfig(s"govuk-tax.assets.version")
  override lazy val analyticsToken: Option[String] = configuration.getString(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString(s"govuk-tax.$env.google-analytics.host").getOrElse("service.gov.uk")

  override lazy val startElement: String = "<table:table-row"
  override lazy val endElement: String = "</table:table-row>"

  override val ggSignInUrl: String = configuration.getString(s"$env.government-gateway-sign-in.host").getOrElse("")
}
