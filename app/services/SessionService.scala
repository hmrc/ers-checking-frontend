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

package services

import models.upscan.{NotStarted, UploadId, UploadStatus, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.CacheUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SessionService extends SessionService {
	override val cacheUtil: CacheUtil = CacheUtil
}

trait SessionService {
	val cacheUtil: CacheUtil
	val CALLBACK_DATA_KEY = "callback_data_key"
	val CALLBACK_DATA_KEY_CSV = "callback_data_key_csv"


	def createCallbackRecordCsv(sessionId: String)
														 (implicit request: Request[_], hc: HeaderCarrier): Future[CacheMap] = {
		cacheUtil.cache(CALLBACK_DATA_KEY_CSV, UpscanCsvFilesCallbackList(List[UpscanCsvFilesCallback]()), sessionId)
	}

	def createCallbackRecord(implicit request: Request[_], hc: HeaderCarrier): Future[CacheMap] = {
		cacheUtil.cache[UploadStatus](CALLBACK_DATA_KEY, NotStarted)
	}

	def updateCallbackRecordCsv(callbackList: UpscanCsvFilesCallbackList, sessionId: String)
														 (implicit request: Request[_], hc: HeaderCarrier): Future[CacheMap] = {
		cacheUtil.cache(CALLBACK_DATA_KEY_CSV, callbackList, sessionId)
	}

	def updateCallbackRecord(uploadStatus: UploadStatus)(implicit request: Request[_], hc: HeaderCarrier): Future[CacheMap] =
		cacheUtil.cache(CALLBACK_DATA_KEY, uploadStatus)

	def getCallbackRecord(implicit request: Request[_], hc: HeaderCarrier): Future[Option[UploadStatus]] =
		cacheUtil.shortLivedCache.fetchAndGetEntry[UploadStatus](cacheUtil.getCacheId, CALLBACK_DATA_KEY)

	def getCallbackRecordCsv(sessionId: String)
													(implicit request: Request[_], hc: HeaderCarrier): Future[UpscanCsvFilesCallbackList] = {
		cacheUtil.fetch[UpscanCsvFilesCallbackList](CALLBACK_DATA_KEY_CSV, sessionId)
	}
}
