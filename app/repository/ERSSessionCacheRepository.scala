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

package repository

import config.ApplicationConfig
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import play.api.libs.json.Writes
import play.api.mvc.Request
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ERSSessionCacheRepository @Inject()(mongoComponent: MongoComponent,
                                          appConfig: ApplicationConfig,
                                          timestampSupport: TimestampSupport
                                         )(implicit ec: ExecutionContext) extends SessionCacheRepository(
  mongoComponent = mongoComponent,
  collectionName = appConfig.appName,
  ttl = Duration(appConfig.mongoTTLInSeconds, TimeUnit.SECONDS),
  timestampSupport = timestampSupport,
  sessionIdKey = SessionKeys.sessionId
)(ec) {

  def put[T: Writes](dataKey: DataKey[T], data: T, sessionId: String): Future[CacheItem] = {
    cacheRepo
      .collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", sessionId),
        update = Updates.combine(
          Updates.set("data." + dataKey.unwrap, Codecs.toBson(data)),
          Updates.set("modifiedDetails.lastUpdated", timestampSupport.timestamp()),
          Updates.setOnInsert("_id", sessionId),
          Updates.setOnInsert("modifiedDetails.createdAt", timestampSupport.timestamp())
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
  }

  def getAllFromSession(implicit request: Request[_]): Future[Option[CacheItem]] = {
    cacheRepo.findById(request)
  }

  def getSession(sessionId: String): Future[Option[CacheItem]] = {
    cacheRepo
      .collection
      .find(Filters.equal("_id", sessionId))
      .toFuture()
      .map(_.headOption)
  }

}
