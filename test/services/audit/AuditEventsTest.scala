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

package services.audit

import controllers.auth.RequestWithOptionalEmpRef
import helpers.ErsTestHelper
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class AuditEventsTest extends WordSpecLike with Matchers with OptionValues with ErsTestHelper {

  val dataEvent: DataEvent = DataEvent(
    auditSource = "ers-checking-frontend",
    auditType = "transactionName",
    eventId = "fakeId",
    tags = Map("test" -> "test"),
    detail = Map("test" -> "details")
  )

  "The auditRunTimeError DataEvent" should {
    class TestException(message: String) extends Throwable(message)

    "include the 'CheckingServiceFileProcessingError' auditType" in {
      val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
      val testAuditEvent = new AuditEvents(mockAuditConnector)
      val eventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
      val testException: TestException = new TestException("testErrorMessage")
      val expectedDataEvent: DataEvent = dataEvent.copy(
        auditType = "CheckingServiceRunTimeError",
        detail = Map(
          "ErrorMessage" -> "testErrorMessage",
          "Context" -> "testContextInfo",
          "sheetName" -> "testSheetName",
          "StackTrace" -> testException.getStackTrace.toString
        )
      )

      testAuditEvent.auditRunTimeError(testException, "testContextInfo", "testSheetName")
      verify(mockAuditConnector, times(1)).sendEvent(eventCaptor.capture())(any(), any())
      expectedDataEvent.auditSource shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditSource
      expectedDataEvent.auditType shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditType
      expectedDataEvent.detail.head shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail.head
      expectedDataEvent.detail.take(1) shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail.take(1)
      expectedDataEvent.detail.take(2) shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail.take(2)
    }
  }

  "The fileProcessingErrorAudit DataEvent" should {
    "include the 'CheckingServiceFileProcessingError' auditType" in {
      val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
      val testAuditEvent = new AuditEvents(mockAuditConnector)
      val eventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
      val expectedDataEvent: DataEvent = dataEvent.copy(
        auditType = "CheckingServiceFileProcessingError",
        detail = Map(
          "schemeType" -> "testScheme",
          "sheetName" -> "testSheetName",
          "ErrorMessage" -> "testErrorMessage")
      )

      testAuditEvent.fileProcessingErrorAudit("testScheme", "testSheetName", "testErrorMessage")
      verify(mockAuditConnector, times(1)).sendEvent(eventCaptor.capture())(any(), any())
      expectedDataEvent.auditSource shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditSource
      expectedDataEvent.auditType shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditType
      expectedDataEvent.detail shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail
    }
  }

  "The numRowsInSchemeData DataEvent" should {
    "include the 'CheckingServiceFileProcessingError' audit type when the request has no EmpRef" in {
      val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
      val testAuditEvent = new AuditEvents(mockAuditConnector)
      implicit val fakeRequest: RequestWithOptionalEmpRef[_] = RequestWithOptionalEmpRef(FakeRequest(), None)
      val eventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
      val expectedDataEvent: DataEvent = dataEvent.copy(
        auditType = "CheckingServiceNumRowsInSchemeData",
        detail = Map(
          "sheetName" -> "testSheetName",
          "rowsWithData" -> "3",
          "empRef" -> ""
        )
      )

      testAuditEvent.numRowsInSchemeData("testSheetName", 3)
      verify(mockAuditConnector, times(1)).sendEvent(eventCaptor.capture())(any(), any())
      expectedDataEvent.auditSource shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditSource
      expectedDataEvent.auditType shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditType
      expectedDataEvent.detail shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail
    }

    "include the 'CheckingServiceFileProcessingError' audit type when the request has an EmpRef" in {
      val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
      val testAuditEvent = new AuditEvents(mockAuditConnector)
      implicit val fakeRequest: RequestWithOptionalEmpRef[_] = RequestWithOptionalEmpRef(FakeRequest(), Some(EmpRef("1234", "GA4567")))
      val eventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
      val expectedDataEvent: DataEvent = dataEvent.copy(
        auditType = "CheckingServiceNumRowsInSchemeData",
        detail = Map(
          "sheetName" -> "testSheetName",
          "rowsWithData" -> "2",
          "empRef" -> "1234/GA4567"
        )
      )

      testAuditEvent.numRowsInSchemeData("testSheetName", 2)
      verify(mockAuditConnector, times(1)).sendEvent(eventCaptor.capture())(any(), any())
      expectedDataEvent.auditSource shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditSource
      expectedDataEvent.auditType shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditType
      expectedDataEvent.detail shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail
    }
  }
}
