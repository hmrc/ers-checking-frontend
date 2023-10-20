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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsObject, JsString, JsValue}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.ExecutionContext

class ERSSessionCacheRepositorySpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach {

  private val sessionId = "sessionId"
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  private implicit val request: Request[AnyRef] = FakeRequest().withSession("sessionId" -> sessionId)

  private val fakeAppConfig = app.injector.instanceOf[ApplicationConfig]
  private val mongoComponent = app.injector.instanceOf[MongoComponent]
  private val timestamp = app.injector.instanceOf[TimestampSupport]
  private val sessionRepository = new ERSSessionCacheRepository(mongoComponent, fakeAppConfig, timestamp)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionRepository.cacheRepo.deleteEntity(request))
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    await(sessionRepository.cacheRepo.deleteEntity(request))
  }

  private def cacheItem(id: String, data: Seq[(String, JsValue)]): CacheItem = CacheItem(
    id = id,
    data = JsObject(data),
    createdAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC),
    modifiedAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC)
  )

  "ERSSessionCacheRepository" when {

    "put" should {
      "update data when id already exists and return a CacheItem" in {
        await(sessionRepository.putSession(DataKey("file-type"), "csv"))

        val result = sessionRepository.put[String](DataKey("file-type"), "ods", sessionId)
        val expectedResult = cacheItem(sessionId, Seq("file-type" -> JsString("ods")))

        await(result).id mustBe expectedResult.id
        await(result).data mustBe expectedResult.data
      }

      "insert data for a new id and return a CacheItem" in {
        val expectedResult = cacheItem(sessionId, Seq("test-key" -> JsString("test-value")))

        val result = sessionRepository.put[String](DataKey("test-key"), "test-value", sessionId)

        await(result).id mustBe expectedResult.id
        await(result).data mustBe expectedResult.data
      }
    }

    "getSession" should {

      "return None when there's no data found" in {
        val result = await(sessionRepository.getSession(sessionId))

        result mustBe None
      }

      "return CacheItem with data when id is found" in {
        await(sessionRepository.put[String](DataKey("file-type"), "ods", sessionId))

        val expectedResult = cacheItem(sessionId, Seq("file-type" -> JsString("ods")))
        val result = sessionRepository.getSession(sessionId)

        await(result).map { cacheItem =>
          cacheItem.id mustBe expectedResult.id
          cacheItem.data mustBe expectedResult.data
        }
      }
    }

    "getAllFromSession" should {
      "return CacheItem with all the data from session" in {
        await(sessionRepository.putSession(DataKey("file-type"), "csv"))
        await(sessionRepository.putSession(DataKey("file-size"), "5MB"))

        val result = sessionRepository.getAllFromSession(request)
        val expectedResult = cacheItem(sessionId, Seq("file-type" -> JsString("csv"), "file-size" -> JsString("5MB")))

        await(result).map { cacheItem =>
          cacheItem.id mustBe expectedResult.id
          cacheItem.data mustBe expectedResult.data
        }
      }

      "return None when there is no data in session" in {
        val result = sessionRepository.getAllFromSession(request)

        await(result) mustBe None
      }
    }
  }

}
