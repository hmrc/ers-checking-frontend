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

package services

import play.api.Logging
import play.api.libs.json
import play.api.libs.json.{Format, JsValue, Reads}
import play.api.mvc.Request
import repository.ERSSessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionCacheService @Inject()(sessionRepository: ERSSessionCacheRepository)
                                   (implicit ec: ExecutionContext) extends Logging {

  def cache[T](key: String, body: T)(implicit request: Request[_], formats: json.Format[T]): Future[(String, String)] =
    sessionRepository.putSession(DataKey(key), body)

  def cache[T](key: String, body: T, sessionId: String)(implicit formats: json.Format[T]): Future[CacheItem] =
    sessionRepository.put[T](DataKey(key), body, sessionId)

  def fetch[T](key: String)(implicit request: Request[_], formats: json.Format[T]): Future[Option[T]] = {
    sessionRepository.getFromSession[T](DataKey(key))
  }

  def fetchKeyFromSession[T](sessionId: String, key: String)(implicit formats: Format[T]): Future[Option[T]] = {
    sessionRepository.getSession(sessionId).map {
      case Some(cacheItem) => getEntry(cacheItem, key)
      case None => None
    }
  }

  @throws(classOf[NoSuchElementException])
  def fetchAndGetEntry[T](key: String)(implicit request: Request[_], hc: HeaderCarrier, formats: json.Format[T]): Future[T] = {
    fetch[JsValue](key).map { res =>
      res.get.as[T]
    } recover {
      case e: NoSuchElementException =>
        logger.warn(s"[SessionCacheService][fetchAndGetEntry] " +
          s"fetch failed to get key $key with exception $e, timestamp: ${java.time.LocalTime.now()}.")
        throw new NoSuchElementException
      case _: Throwable =>
        logger.error(s"[SessionCacheService][fetchAndGetEntry] " +
          s"fetch failed to get key $key for ${hc.sessionId} with exception, timestamp: ${java.time.LocalTime.now()}.")
        throw new Exception
    }
  }

  @throws(classOf[NoSuchElementException])
  def fetchAll()(implicit request: Request[_]): Future[CacheItem] = {

    sessionRepository.getAllFromSession.map { res =>
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

  def getEntry[T](cacheItem: CacheItem, key: String)(implicit reads: Reads[T]): Option[T] = {
    cacheItem.data.value.get(key).map(json =>
      json.validate[T].fold(
        errors => throw new InternalServerException(s"CacheItem entry for $key could not be parsed, errors: $errors"),
        valid => valid
      )
    )
  }

  def removeByKey[T](key: String)(implicit request: Request[_]): Future[Unit] =
    sessionRepository.deleteFromSession(DataKey(key))

}
