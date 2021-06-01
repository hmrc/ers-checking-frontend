/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ERSShortLivedCache
import play.api.Logger
import play.api.libs.json
import play.api.libs.json.JsValue
import play.api.mvc.Request
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait CacheUtil {
  val shortLivedCache: ERSShortLivedCache
  val logger: Logger

  // Cache Ids
  val SCHEME_CACHE: String = "scheme-type"
  val FILE_TYPE_CACHE: String = "check-file-type"
  val SCHEME_ERROR_COUNT_CACHE: String = "scheme-error-count"
  val FILE_NAME_NO_EXTN_CACHE: String = "file-name-no-extn"
  val ERROR_LIST_CACHE: String = "error-list"
  val ERROR_SUMMARY_CACHE: String = "error-summary"
  val FORMAT_ERROR_CACHE: String = "format_error"
  val FORMAT_ERROR_CACHE_PARAMS: String = "format_error_params"
  val FORMAT_ERROR_EXTENDED_CACHE: String = "format_extended_error"
  val FILE_NAME_CACHE: String = "file-name"
  val CSV_FILES_UPLOAD: String = "csv-files-upload"

  def cache[T](key: String, body: T)
              (implicit hc: HeaderCarrier, ec: ExecutionContext, formats: json.Format[T]): Future[CacheMap] = {
    shortLivedCache.cache[T](getCacheId, key, body)
  }

  def cache[T](key: String, body: T, cacheId: String)
              (implicit hc: HeaderCarrier, ec: ExecutionContext, formats: json.Format[T]): Future[CacheMap] = {
    shortLivedCache.cache[T](cacheId, key, body)
  }

  def remove(cacheId: String)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[HttpResponse] = {
    shortLivedCache.remove(cacheId)
  }

  @throws(classOf[NoSuchElementException])
  def fetch[T](key: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: json.Format[T]): Future[T] = {
    shortLivedCache.fetchAndGetEntry[JsValue](getCacheId, key).map { res =>
      res.get.as[T]
    } recover {
      case e: NoSuchElementException =>
        logger.warn(s"[CacheUtil][fetch] fetch failed to get key $key with exception $e, timestamp: ${java.time.LocalTime.now()}.")
        throw new NoSuchElementException
      case _: Throwable =>
        logger.error(s"[CacheUtil][fetch] fetch failed to get key $key for ${hc.sessionId} with exception, timestamp: ${java.time.LocalTime.now()}.")
        throw new Exception
    }
  }

  @throws(classOf[NoSuchElementException])
  def fetch[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: json.Format[T]): Future[T] = {
    shortLivedCache.fetchAndGetEntry[JsValue](cacheId, key).map { res =>
      res.get.as[T]
    } recover {
      case e: NoSuchElementException =>
        logger.warn(s"[CacheUtil][fetch] fetch with 2 params failed to get key [$key] for cacheId [$cacheId] with exception - $e")
        throw new NoSuchElementException
      case t: Throwable =>
        logger.error(s"[CacheUtil][fetch] fetch with 2 params failed to get key [$key] for cacheId [$cacheId] with exception - $t")
        throw new Exception
    }
  }

  @throws(classOf[NoSuchElementException])
  def fetchAll()(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[AnyRef]): Future[CacheMap] = {

    shortLivedCache.fetch(getCacheId).map { res =>
      try {
        val sessionMap = res.get
        sessionMap

      } catch {
        case e: NoSuchElementException =>
          logger.warn(s"[CacheUtil][fetchAll] failed to get all keys with exception. " +
            s"Method: ${request.method} req: ${request.path}, param: ${request.rawQueryString}", e)

          throw new NoSuchElementException
        case t: Throwable =>
          logger.error(s"[CacheUtil][fetchAll] failed to get all keys with exception. " +
            s"Method: ${request.method} req: ${request.path}, param: ${request.rawQueryString}", t)
          throw new Exception
      }
    } recover {
      case e: NoSuchElementException =>
        logger.error(s"[CacheUtil][fetchAll] failed to get all keys with " +
          s"exception ${e.getMessage} method: ${request.method}  req: ${request.path}, param: ${request.rawQueryString}", e)
        throw new Exception
    }
  }

  def getCacheId(implicit hc: HeaderCarrier): String = {
    hc.sessionId.getOrElse(throw new RuntimeException("")).value
  }
}
