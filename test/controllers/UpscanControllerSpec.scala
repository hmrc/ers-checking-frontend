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

package controllers

import controllers.auth.AuthAction
import helpers.WithMockedAuthActions
import models.upscan.{InProgress, NotStarted, UploadId, UploadStatus, UploadedSuccessfully, UpscanCallback, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList, UpscanCsvFilesList, UpscanIds}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import services.{CsvFileProcessor, ProcessODSService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.CacheUtil

import scala.concurrent.Future

class UpscanControllerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with I18nSupport with WithMockedAuthActions {

  val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

  implicit val hc: HeaderCarrier = new HeaderCarrier

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build
  def injector: Injector = app.injector
  implicit val messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
  implicit val request: Request[_] = FakeRequest()
  val mockAuthAction : AuthAction = mock[AuthAction]
  val mockSessionService: SessionService = mock[SessionService]
  val mockProcessODSService: ProcessODSService = mock[ProcessODSService]
  val mockCsvFileProcessor: CsvFileProcessor = mock[CsvFileProcessor]
  val mockCacheUtil: CacheUtil = mock[CacheUtil]

  val upscanController = new UpscanController {
    override val authAction: AuthAction = mockAuthAction
    override val sessionService: SessionService = mockSessionService
    override val processODSService: ProcessODSService = mockProcessODSService
    override val processCSVService: CsvFileProcessor = mockCsvFileProcessor
    mockAnyContentAction
  }

  "failure" should {
    "return a 200 when successful" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val result: Future[Result] = upscanController.failure().apply(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "successCsv" should {
    val uploadId = UploadId("uploadId")

    "redirect to the upload csv file page when the file file status is uploaded successfully" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val successfully: UploadedSuccessfully = UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))
      val upscanId: UpscanIds = UpscanIds(uploadId, "fileId", NotStarted)
      val singleCsvFile: UpscanCsvFilesList = UpscanCsvFilesList(ids = Seq(upscanId))
      val upscanCsvFilesCallback= UpscanCsvFilesCallback(uploadId: UploadId, successfully)
      val upscanCsvFilesListCallbackList = UpscanCsvFilesCallbackList(files = List(upscanCsvFilesCallback))

      when(mockSessionService.cacheUtil).thenReturn(mockCacheUtil)
      when(mockCacheUtil.fetch[UpscanCsvFilesList](any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(singleCsvFile))
      when(mockSessionService.cacheUtil).thenReturn(mockCacheUtil)
      when(mockCacheUtil.cache(any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(null))
      when(mockSessionService.getCallbackRecordCsv(any())(any(), any())).thenReturn(Future.successful(upscanCsvFilesListCallbackList))

      val result = upscanController.successCSV(uploadId, Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.UploadController.uploadCSVFile(Fixtures.getMockSchemeTypeString).toString
    }

    "return a 200 when a file is still in progress" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val upscanId: UpscanIds = UpscanIds(uploadId, "fileId", NotStarted)
      val singleCsvFile: UpscanCsvFilesList = UpscanCsvFilesList(ids = Seq(upscanId))
      val upscanCsvFilesCallback= UpscanCsvFilesCallback(uploadId: UploadId, InProgress)
      val upscanCsvFilesListCallbackList = UpscanCsvFilesCallbackList(files = List(upscanCsvFilesCallback))

      when(mockSessionService.cacheUtil).thenReturn(mockCacheUtil)
      when(mockCacheUtil.fetch[UpscanCsvFilesList](any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(singleCsvFile))
      when(mockSessionService.cacheUtil).thenReturn(mockCacheUtil)
      when(mockCacheUtil.cache(any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(null))
      when(mockSessionService.getCallbackRecordCsv(any())(any(), any())).thenReturn(Future.successful(upscanCsvFilesListCallbackList))

      val result = upscanController.successCSV(uploadId, Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return a 200 when an exception occurs" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val upscanId: UpscanIds = UpscanIds(uploadId, "fileId", NotStarted)
      val singleCsvFile: UpscanCsvFilesList = UpscanCsvFilesList(ids = Seq(upscanId))

      when(mockSessionService.cacheUtil).thenReturn(mockCacheUtil)
      when(mockCacheUtil.fetch[UpscanCsvFilesList](any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(singleCsvFile))
      when(mockSessionService.cacheUtil).thenReturn(mockCacheUtil)
      when(mockCacheUtil.cache(any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(null))
      when(mockSessionService.getCallbackRecordCsv(any())(any(), any())).thenReturn(Future.failed(new Exception("error")))

      val result = upscanController.successCSV(uploadId, Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "successOds" should {
    "redirect to the upload ods file page when the file file status is uploaded successfully" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val successfully: UploadedSuccessfully = UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))

      when(mockSessionService.getCallbackRecord(any(), any())).thenReturn(Future.successful(Some(successfully)))

      val result = upscanController.successODS(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.UploadController.uploadODSFile(Fixtures.getMockSchemeTypeString).toString

    }

    "return a 200 when a when getCallbackRecord returns a None" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      when(mockSessionService.getCallbackRecord(any(), any())).thenReturn(Future.successful(None))

      val result = upscanController.successODS(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return a 200 when a when an exception occurs" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      when(mockSessionService.getCallbackRecord(any(), any())).thenReturn(Future.failed(new Exception("an error occured")))

      val result = upscanController.successODS(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
