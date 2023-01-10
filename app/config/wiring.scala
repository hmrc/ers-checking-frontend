/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.cache.client.{ShortLivedCache, ShortLivedHttpCaching}

@Singleton
class ERSShortLivedHttpCache @Inject()(val http: HttpClient,
                                       appConfig: ApplicationConfig
                                      ) extends ShortLivedHttpCaching {
  override lazy val defaultSource: String = appConfig.appName
  lazy val baseUri: String = appConfig.shortLivedCacheBaseUri
  lazy val domain: String = appConfig.shortLivedCacheDomain
}

@Singleton
class ERSShortLivedCache @Inject()(val http: HttpClient,
                                   appConfig: ApplicationConfig,
                                   val configuration: Configuration
                                  ) extends ShortLivedCache {
  override def shortLiveCache: ShortLivedHttpCaching = new ERSShortLivedHttpCache(http, appConfig)
  override implicit lazy val crypto: Encrypter with Decrypter = new ApplicationCrypto(configuration.underlying).JsonCrypto
}

