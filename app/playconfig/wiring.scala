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

package playconfig

import play.Logger
import play.api.Play
import services.AllWsHttp
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.cache.client.{ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartialRetriever {
  override val httpGet = AllWsHttp
}

object ERSAuthConnector extends AuthConnector with ServicesConfig {
  protected def mode: play.api.Mode.Mode = Play.current.mode
  protected def runModeConfiguration: play.api.Configuration = Play.current.configuration
  protected def appNameConfiguration: play.api.Configuration = runModeConfiguration

  Logger.info("Getting the authorisation")
  val serviceUrl = baseUrl("auth")
  Logger.info("got the ServiceURL " + serviceUrl)
  lazy val http = AllWsHttp
}

object ERSAuditConnector extends Auditing {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object ShortLivedHttpCaching extends ShortLivedHttpCaching with AppName with ServicesConfig {
  override protected def mode: play.api.Mode.Mode = Play.current.mode
  override protected def runModeConfiguration: play.api.Configuration = Play.current.configuration
  override protected def appNameConfiguration: play.api.Configuration = runModeConfiguration

  override lazy val http = AllWsHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.short-lived-cache")
  override lazy val domain = getConfString("cachable.short-lived-cache.domain", throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

object ShortLivedCache extends ShortLivedCache {
  override implicit lazy val crypto = new ApplicationCrypto(Play.current.configuration.underlying).JsonCrypto
  override lazy val shortLiveCache = ShortLivedHttpCaching
}
