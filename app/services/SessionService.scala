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

package services

import models.upscan.{NotStarted, UploadStatus, UpscanCsvFilesCallbackList}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionService @Inject()(val ersUtil: ERSUtil) {
	val CALLBACK_DATA_KEY = "callback_data_key"
	val CALLBACK_DATA_KEY_CSV = "callback_data_key_csv"

	def createCallbackRecord(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
		ersUtil.cache[UploadStatus](CALLBACK_DATA_KEY, NotStarted)
	}

	def createCallbackRecordCSV(callbackList: UpscanCsvFilesCallbackList, sessionId: String)
														 (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
		ersUtil.cache(CALLBACK_DATA_KEY_CSV, callbackList, sessionId)
	}

	def updateCallbackRecord(uploadStatus: UploadStatus)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
		ersUtil.cache(CALLBACK_DATA_KEY, uploadStatus)

	def getCallbackRecord(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UploadStatus]] =
		ersUtil.shortLivedCache.fetchAndGetEntry[UploadStatus](ersUtil.getCacheId, CALLBACK_DATA_KEY)
}
