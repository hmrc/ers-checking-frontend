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

import models.upscan.UploadedSuccessfully
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsBoolean, JsObject, JsString, JsValue}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repository.ERSSessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

import java.time.{LocalDate, ZoneOffset}
import java.util.NoSuchElementException
import scala.concurrent.{ExecutionContext, Future}

class SessionCacheServiceSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private val sessionId = "sessionId"
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  private implicit val request: Request[AnyRef] = FakeRequest().withSession("sessionId" -> sessionId)
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockSessionRepository: ERSSessionCacheRepository = mock[ERSSessionCacheRepository]
  private val mockSessionCacheService = mock[SessionCacheService]
  private val testService = new SessionCacheService(mockSessionRepository)(ec)

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(mockSessionRepository)
    reset(mockSessionCacheService)
  }

  private def cacheItem(id: String, data: Seq[(String, JsValue)] = Seq("" -> JsString(""))): CacheItem =
    CacheItem(
      id = id,
      data = JsObject(data),
      createdAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC),
      modifiedAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC)
    )

  "SessionCacheService" when {
    "cache - 2 params" should {
      "return a Future[(String, String)]" in {
        when(mockSessionRepository.putSession(DataKey(anyString()), any())(any(), any(), any()))
          .thenReturn(Future.successful(("test-value", sessionId)))

        val result = testService.cache("file-type", "csv")

        await(result) mustBe("test-value", sessionId)
      }
    }

    "cache - 3 params" should {
      "return a CacheItem" in {
        val data = cacheItem(sessionId, Seq("file-size" -> JsString("5MB")))
        when(mockSessionRepository.put(DataKey(anyString()), any(), any())(any())).thenReturn(Future.successful(data))

        val result = testService.cache("file-size", "5MB", sessionId)
        await(result) mustBe data
      }

    }

    "fetch" should {
      "return data from session" in {
        when(mockSessionRepository.getFromSession[String](DataKey(anyString()))(any(), any())).thenReturn(Future.successful(Some("some-value")))

        val result = testService.fetch[String]("some-key")

        await(result) mustBe Some("some-value")
      }

      "return None when there's no data in session" in {
        when(mockSessionRepository.getFromSession[String](DataKey(anyString()))(any(), any())).thenReturn(Future.successful(None))

        val result = testService.fetch[String]("some-key")

        await(result) mustBe None
      }
    }

    "fetchAndGetEntry" should {
      "return data from session" in {
        val uploadedSuccessfully = UploadedSuccessfully("some-name", "some-downloadUrl")
        when(mockSessionRepository.getFromSession[UploadedSuccessfully](DataKey(anyString()))(any(), any()))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockSessionCacheService.fetch[UploadedSuccessfully](any())(any(), any())).thenReturn(Future.successful(Some(uploadedSuccessfully)))

        val result = testService.fetchAndGetEntry[UploadedSuccessfully](sessionId)(request, hc, UploadedSuccessfully.uploadedSuccessfullyFormat)

        result.map { successValue =>
          successValue mustBe uploadedSuccessfully
        }
      }

      "return an exception when fetch returns None" in {
        when(mockSessionRepository.getFromSession[UploadedSuccessfully](DataKey(anyString()))(any(), any()))
          .thenReturn(Future.successful(None))
        when(mockSessionCacheService.fetch[UploadedSuccessfully](any())(any(), any())).thenReturn(Future.successful(None))

        val result = testService.fetchAndGetEntry[UploadedSuccessfully](sessionId)(request, hc, UploadedSuccessfully.uploadedSuccessfullyFormat)

        result.failed.map { exception =>
          exception mustBe an[NoSuchElementException]
        }
      }

      "return an exception when fetch throws an exception" in {
        val uploadedSuccessfully = UploadedSuccessfully("some-name", "some-downloadUrl")
        when(mockSessionRepository.getFromSession[UploadedSuccessfully](DataKey(anyString()))(any(), any()))
          .thenReturn(Future.successful(Some(uploadedSuccessfully)))
        when(mockSessionCacheService.fetch[UploadedSuccessfully](any())(any(), any())).thenReturn(Future.failed(new RuntimeException()))

        val result = testService.fetchAndGetEntry[UploadedSuccessfully](sessionId)(request, hc, UploadedSuccessfully.uploadedSuccessfullyFormat)

        result.failed.map { exception =>
          exception mustBe an[Exception]
        }
      }

    }

    "fetchKeyFromSession" should {
      "return None when there's no session data found for sessionId" in {
        when(mockSessionRepository.getSession(any())).thenReturn(Future.successful(None))

        val result = testService.fetchKeyFromSession[UploadedSuccessfully](sessionId,
          "key")(UploadedSuccessfully.uploadedSuccessfullyFormat)

        await(result) mustBe None
      }

      "return None when key is not found in the data" in {
        val data = cacheItem(sessionId, Seq("key" -> JsString("value")))
        when(mockSessionRepository.getSession(any())).thenReturn(Future.successful(Some(data)))


        val result = testService.fetchKeyFromSession(sessionId,
          "different-key")(UploadedSuccessfully.uploadedSuccessfullyFormat)

        await(result) mustBe None
      }

      "return Some when data is found" in {
        val data = cacheItem(sessionId, Seq("name" -> JsString("some-name"), "downloadUrl" -> JsString("some-url")))
        val uploadedSuccessfully = UploadedSuccessfully("some-name", "some-url")

        when(mockSessionRepository.getSession(any())).thenReturn(Future.successful(Some(data)))
        when(mockSessionCacheService.getEntry[UploadedSuccessfully](ArgumentMatchers.eq(data), any())(any()))
          .thenReturn(Some(uploadedSuccessfully))

        val result = testService.fetchKeyFromSession[UploadedSuccessfully](sessionId,
          "some-name")(UploadedSuccessfully.uploadedSuccessfullyFormat)

        await(result).map { successValue =>
          successValue mustBe uploadedSuccessfully
        }

      }

      "return an InternalServerException when there's a problem validating json" in {
        val data = cacheItem(sessionId, Seq("key" -> JsString("value")))
        when(mockSessionRepository.getSession(any())).thenReturn(Future.successful(Some(data)))

        val result = testService.fetchKeyFromSession[UploadedSuccessfully](sessionId,
          "key")(UploadedSuccessfully.uploadedSuccessfullyFormat)

        result.failed.map { exception =>
          exception mustBe an[InternalServerException]
        }
      }
    }

    "fetchAll" should {
      "return CacheItem when there's data" in {
        val data = cacheItem(sessionId, Seq("key1" -> JsString("value1"), "key2" -> JsBoolean(true), "key3" -> JsBoolean(false)))
        when(mockSessionRepository.getAllFromSession(request)).thenReturn(Future.successful(Some(data)))

        val result = testService.fetchAll()(request)

        await(result) mustBe data
      }

      "throw an exception when there's no data in session cache repository" in {
        when(mockSessionRepository.getAllFromSession(request)).thenReturn(Future.successful(None))

        val result = testService.fetchAll()(request)

        result.failed.map { throwable =>
          throwable mustBe a[Exception]
        }
      }

      "throw an exception when getAllFromSession returns NoSuchElementException" in {
        when(mockSessionRepository.getAllFromSession(request)).thenReturn(Future.failed(new NoSuchElementException()))

        val result = testService.fetchAll()(request)

        result.failed.map { throwable =>
          throwable mustBe a[Exception]
        }
      }
    }

    "getEntry" must {
      "return None when key isn't found" in {
        val data = cacheItem(sessionId, Seq("name" -> JsString("some-name"), "downloadUrl" -> JsString("some-url")))
        val result = testService.getEntry[UploadedSuccessfully](data, "key-not-found")

        result mustBe None
      }
    }

    "removeByKey" should {
      "remove key and value from the session" in {
        when(mockSessionRepository.deleteFromSession(DataKey(anyString()))(any())).thenReturn(Future.successful(()))

        await(testService.removeByKey("some-key")(request)) mustBe ()
      }
    }
  }

}
