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

package controllers

import helpers.ErsTestHelper
import models.upscan._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{DefaultMessagesControllerComponents, Request, Result}
import services.{ProcessCsvService, ProcessODSService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class UpscanControllerSpec extends UnitSpec with ErsTestHelper with GuiceOneAppPerSuite {

  val config: Map[String, Any] = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(app)
  val mockProcessODSService: ProcessODSService = mock[ProcessODSService]
  val mockProcessCsvService: ProcessCsvService = mock[ProcessCsvService]

  val uploadId: UploadId = UploadId("uploadId")
  val upscanCsvFilesCallback: UpscanCsvFilesCallback = UpscanCsvFilesCallback(uploadId: UploadId, InProgress)
  val upscanCsvFilesListCallbackList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(files = List(upscanCsvFilesCallback))

  def upscanController(csvList: UpscanCsvFilesCallbackList = upscanCsvFilesListCallbackList): UpscanController =
    new UpscanController(mockAuthAction, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
    override def fetchCsvCallbackList(list: UpscanCsvFilesList, sessionId: String)
                                     (implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[UpscanCsvFilesCallback]] = {
      Future.successful(csvList.files)
    }
    mockAnyContentAction
  }

  "failure" should {
    "return a 500 when called" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val result: Future[Result] = upscanController().failure().apply(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "successCsv" should {
    "redirect to the upload csv file page when the file file status is uploaded successfully" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val successfully: UploadedSuccessfully = UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))
      val upscanId: UpscanIds = UpscanIds(uploadId, "fileId", NotStarted)
      val singleCsvFile: UpscanCsvFilesList = UpscanCsvFilesList(ids = Seq(upscanId))
      val upscanCsvFilesCallback= UpscanCsvFilesCallback(uploadId: UploadId, successfully)
      val upscanCsvFilesListCallbackList = UpscanCsvFilesCallbackList(files = List(upscanCsvFilesCallback))

      when(mockErsUtil.fetch[UpscanCsvFilesList](any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(singleCsvFile))
      when(mockErsUtil.cache(any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(null))

      val result = upscanController(upscanCsvFilesListCallbackList).successCSV(uploadId, Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.UploadController.uploadCSVFile(Fixtures.getMockSchemeTypeString).toString
    }

    //TODO maybe consider showing the user a separate page that tells them it's still in progress instead of a global error page?
    "send a user to the global error page when a file is still in progress" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val upscanId: UpscanIds = UpscanIds(uploadId, "fileId", NotStarted)
      val singleCsvFile: UpscanCsvFilesList = UpscanCsvFilesList(ids = Seq(upscanId))

      when(mockSessionService.ersUtil).thenReturn(mockErsUtil)
      when(mockErsUtil.fetch[UpscanCsvFilesList](any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(singleCsvFile))
      when(mockErsUtil.cache(any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(null))

      val result = upscanController(upscanCsvFilesListCallbackList).successCSV(uploadId, Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return a 500 when an exception occurs" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val upscanId: UpscanIds = UpscanIds(uploadId, "fileId", NotStarted)
      val singleCsvFile: UpscanCsvFilesList = UpscanCsvFilesList(ids = Seq(upscanId))

      when(mockSessionService.ersUtil).thenReturn(mockErsUtil)
      when(mockErsUtil.fetch[UpscanCsvFilesList](any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(singleCsvFile))
      when(mockErsUtil.cache(any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(null))

      def upscanControllerError(): UpscanController =
        new UpscanController(mockAuthAction, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
          override def fetchCsvCallbackList(list: UpscanCsvFilesList, sessionId: String)
                                           (implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[UpscanCsvFilesCallback]] = {
            Future.failed(new Exception("error"))
          }
          mockAnyContentAction
        }

      val result = upscanControllerError().successCSV(uploadId, Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "successOds" should {
    "redirect to the upload ods file page when the file status is uploaded successfully" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      val successfully: UploadedSuccessfully = UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))

      when(mockSessionService.getCallbackRecord(any(), any(), any())).thenReturn(Future.successful(Some(successfully)))

      val result = upscanController().successODS(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.UploadController.uploadODSFile(Fixtures.getMockSchemeTypeString).toString

    }

    "return a 500 (getGlobalErrorPage) when getCallbackRecord returns a None" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      when(mockSessionService.getCallbackRecord(any(), any(), any())).thenReturn(Future.successful(None))

      val result = upscanController().successODS(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return a 500 (getGlobalErrorPage) when an exception occurs" in {
      val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
      when(mockSessionService.getCallbackRecord(any(), any(), any())).thenReturn(Future.failed(new Exception("an error occured")))

      val result = upscanController().successODS(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}