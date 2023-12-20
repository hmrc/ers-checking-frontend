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

import play.api.{Configuration, Logging}
import play.api.libs.json
import play.api.libs.json.Reads
import play.api.mvc.Request
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErsCheckingFrontendSessionCacheRepository @Inject()(mongoComponent: MongoComponent,
                                                          configuration: Configuration)
                                                         (implicit ec: ExecutionContext)
    extends SessionCacheRepository(
      mongoComponent = mongoComponent,
      collectionName   = "sessions",
      ttl              = Duration(configuration.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS),
      timestampSupport =  new CurrentTimestampSupport(),
      sessionIdKey = SessionKeys.sessionId
    ) with Logging {

  def cache[T](key: String, body: T)(implicit request: Request[_], formats: json.Format[T]): Future[(String, String)] =
    putSession(DataKey(key), body)

  def fetch[T](key: String)(implicit request: Request[_], formats: json.Format[T]): Future[Option[T]] =
    getFromSession[T](DataKey(key))

  def delete[T](key: String)(implicit request: Request[_]): Future[Unit] =
    deleteFromSession(DataKey(key))

  def getAllFromSession()(implicit request: Request[Any]): Future[Option[CacheItem]] =
    cacheRepo.findById(request)

  @throws(classOf[NoSuchElementException])
  def fetchAndGetEntry[T](key: String)(implicit request: Request[_], reads: Reads[T]): Future[T] = {
    getFromSession[T](DataKey[T](key)) map {
      case Some(value) => value
      case None =>
        logger.warn(s"[SessionCacheService][fetchAndGetEntry] " +
          s"fetch failed to get key $key, timestamp: ${java.time.LocalTime.now()}.")
        throw new NoSuchElementException
    }
  }

  @throws(classOf[NoSuchElementException])
  def fetchAll()(implicit request: Request[_]): Future[CacheItem] =
    getAllFromSession().map { res =>
      try {
        val sessionMap = res.get
        sessionMap
      } catch {
        case e: NoSuchElementException =>
          logger.warn(s"[SessionCacheService][fetchAll] failed to get all keys with exception. " +
            s"Method: ${request.method} req: ${request.path}, param: ${request.rawQueryString}", e)
          throw new NoSuchElementException
        case t: Throwable =>
          logger.error(s"[SessionCacheService][fetchAll] failed to get all keys with exception. " +
            s"Method: ${request.method} req: ${request.path}, param: ${request.rawQueryString}", t)
          throw new Exception
      }
    } recover {
      case e: NoSuchElementException =>
        logger.error(s"[SessionCacheService][fetchAll] failed to get all keys with " +
          s"exception ${e.getMessage} method: ${request.method}  req: ${request.path}, param: ${request.rawQueryString}", e)
        throw new Exception
    }

}
