/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Logger
import play.api.libs.json
import play.api.libs.json.JsValue
import play.api.mvc.Request
import uk.gov.hmrc.http.cache.client.ShortLivedCache
import uk.gov.hmrc.http.cache.client.CacheMap
import scala.concurrent.{ExecutionContext}
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object CacheUtil extends CacheUtil {
  def shortLivedCache = playconfig.ShortLivedCache
}

trait CacheUtil {

  // Cache Ids
  val SCHEME_CACHE: String = "scheme-type"      
  val FILE_TYPE_CACHE: String = "check-file-type"     
  val SCHEME_ERROR_COUNT_CACHE: String = "scheme-error-count"
  val FILE_NAME_NO_EXTN_CACHE: String = "file-name-no-extn"
  val ERROR_LIST_CACHE: String = "error-list"
  val ERROR_SUMMARY_CACHE: String = "error-summary"
  val FORMAT_ERROR_CACHE: String = "format_error"
  val FORMAT_ERROR_EXTENDED_CACHE: String = "format_extended_error"
  val FILE_NAME_CACHE: String = "file-name"
    
  private val sourceId : String = "ers-eoy"

  def shortLivedCache: ShortLivedCache

  def cache[T](key:String, body:T)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]) = {
    shortLivedCache.cache[T](getCacheId, key, body)
  }

  def cache[T](key:String, body:T, cacheId : String)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]) = {
    shortLivedCache.cache[T](cacheId, key, body)
  }

  @throws(classOf[NoSuchElementException])
  def fetch[T](key:String)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]): Future[T] = {
    shortLivedCache.fetchAndGetEntry[JsValue](getCacheId, key).map{ res =>
      res.get.as[T]
    }recover{
      case e: NoSuchElementException => {
        throw new NoSuchElementException
      }
      case _ : Throwable => {
        Logger.error(s"fetch failed to get key $key for $getCacheId with exception, timestamp: ${System.currentTimeMillis()}.")
        throw new Exception
      }
    }
  }

  @throws(classOf[NoSuchElementException])
  def fetch[T](key:String, cacheId: String)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]): Future[T] = {
    shortLivedCache.fetchAndGetEntry[JsValue](cacheId, key).map{ res =>
      res.get.as[T]
    }recover{
      case e:NoSuchElementException => {
        throw new NoSuchElementException
      }
      case _ : Throwable => {
        Logger.error(s"fetch with 2 params failed to get key $key for $cacheId with exception, timestamp: ${System.currentTimeMillis()}.")
        throw new Exception
      }
    }
  }

  @throws(classOf[NoSuchElementException])
  def fetchAll()(implicit hc:HeaderCarrier, ec:ExecutionContext, request: Request[AnyRef]): Future[CacheMap] = {

    shortLivedCache.fetch(getCacheId).map { res =>
      try {
        val sessionMap = res.get
        sessionMap

      } catch {
        case e: NoSuchElementException => {
          throw new NoSuchElementException
        }
        case _: Throwable => {
          Logger.error(s"fetchAll failed to get all keys with exception. Method: ${request.method} req: ${request.path}, param: ${request.rawQueryString}")
          throw new Exception
        }
      }
    }recover{
      case e:NoSuchElementException => {
        Logger.error(s"fetchAll failed to get all keys with exception ${e.getMessage} method: ${request.method}  req: ${request.path}, param: ${request.rawQueryString}", e)
        throw new Exception
      }
    }
  }

  private def getCacheId (implicit hc: HeaderCarrier): String = {
    hc.sessionId.getOrElse(throw new RuntimeException("")).value
  }
}
