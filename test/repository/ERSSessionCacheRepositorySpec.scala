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

import models.upscan.UploadedSuccessfully
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Configuration
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.{LocalDate, ZoneOffset}
import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class ERSSessionCacheRepositorySpec
  extends PlaySpec
    with GuiceOneServerPerSuite
    with BeforeAndAfterEach
    with MockitoSugar
    with MongoSupport {

  val mockConfiguration: Configuration = mock[Configuration]
  val sessionCacheRepo = new ErsCheckingFrontendSessionCacheRepository(
    mongoComponent = mongoComponent,
    configuration = mockConfiguration
  )

  val sessionId = "sessionId"
  implicit val request: Request[AnyRef] = FakeRequest().withSession(("sessionId", sessionId))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionCacheRepo.cacheRepo.deleteEntity(request))
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
  }

  def generateTestCacheItem(id: String = "id",
                            data: JsObject = JsObject(Seq("" -> JsString("")))
                           ): CacheItem =
    CacheItem(
      id = id,
      data = data,
      createdAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC),
      modifiedAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC)
    )

  "ERSSessionCacheRepository" when {

    "getAllFromSession" should {
      "return CacheItem with all the data from session" in {

        val sessionId: SessionId = SessionId(UUID.randomUUID().toString)
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(("sessionId", sessionId.toString))

        await(sessionCacheRepo.putSession(DataKey("file-type"), "csv"))
        await(sessionCacheRepo.putSession(DataKey("file-size"), "5MB"))

        val result = sessionCacheRepo.getAllFromSession()

        await(result).map { cacheItem: CacheItem =>
          cacheItem.id mustBe sessionId.toString
          cacheItem.data mustBe JsObject(Seq("file-type" -> JsString("csv"), "file-size" -> JsString("5MB")))
        }
      }

      "return None when there is no data in session" in {
        val sessionId: SessionId = SessionId(UUID.randomUUID().toString)
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(("sessionId", sessionId.toString))
        val result = sessionCacheRepo.getAllFromSession()

        await(result) mustBe None
      }
    }

    "clear" should {
      "delete all data for session" in {
        val sessionId: SessionId = SessionId(UUID.randomUUID().toString)
        implicit val request: Request[AnyRef] = FakeRequest().withSession("sessionId" -> sessionId.toString)

        await(sessionCacheRepo.putSession(DataKey("file-type"), "csv"))
        await(sessionCacheRepo.putSession(DataKey("file-size"), "5MB"))

        await(sessionCacheRepo.delete("file-type")) mustBe ()
        await(sessionCacheRepo.delete("file-size")) mustBe ()
        await(sessionCacheRepo.getAllFromSession()).map(_.data) mustBe Some(JsObject(Seq()))
      }
    }

    "fetchAndGetEntry" should {
      "return cached data from session" in {
        val uploadedSuccessfully: UploadedSuccessfully = UploadedSuccessfully("some-name", "some-downloadUrl")

        sessionCacheRepo.cache[UploadedSuccessfully](sessionId, uploadedSuccessfully)

        val result: UploadedSuccessfully = Await.result(sessionCacheRepo.fetchAndGetEntry[UploadedSuccessfully](sessionId), Duration.Inf)

        result mustBe uploadedSuccessfully
      }

      "return an exception when key dosnt exist" in {
        assertThrows[NoSuchElementException](
          Await.result(sessionCacheRepo.fetchAndGetEntry[UploadedSuccessfully]("SUPER_RANDOM"), Duration.Inf)
        )
      }
    }

    "fetchAll" should {
      "return CacheItem when there's data" in {
        val firstKey = "key1"
        val firstValue = "value1"
        val secondKey = "key2"
        val secondValue = true
        val thirdKey = "key3"
        val thirdValue = false
        val expectedData: JsObject = JsObject(
          Seq(
            firstKey -> JsString(firstValue),
            secondKey -> JsBoolean(secondValue),
            thirdKey -> JsBoolean(thirdValue)
          )
        )
        val maybeFullCache: Future[CacheItem] = for {
          _ <- sessionCacheRepo.cache[String](firstKey, firstValue)
          _ <- sessionCacheRepo.cache[Boolean](secondKey, secondValue)
          _ <- sessionCacheRepo.cache[Boolean](thirdKey, thirdValue)
          cacheItem <- sessionCacheRepo.fetchAll()
        } yield cacheItem

        val cacheItem: CacheItem = Await.result(maybeFullCache, Duration.Inf)
        cacheItem.id must be(sessionId)
        cacheItem.data must be(expectedData)
      }

      "throw an exception when there's no data in session cache repository" in {
        assertThrows[Exception](
          Await.result(sessionCacheRepo.fetchAll(), Duration.Inf)
        )
      }

      "throw an exception when getAllFromSession returns NoSuchElementException" in {
        when(sessionCacheRepo.getAllFromSession()).thenReturn(Future.failed(new NoSuchElementException()))

        val result = sessionCacheRepo.fetchAll()

        result.failed.map { throwable =>
          throwable mustBe a[NoSuchElementException]
        }
      }
    }

  }

}
