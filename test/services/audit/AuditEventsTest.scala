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
//import models.{SchemeInfo, ValidationErrorData}

import controllers.Fixtures
import org.scalatest.{Matchers, WordSpec}
import play.api.Play
import play.api.test.{FakeApplication, FakeRequest}
import services.audit.{AuditEvents, AuditService, AuditServiceConnector}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.collection.mutable.ListBuffer
import uk.gov.hmrc.http.HeaderCarrier

class AuditEventsTest    extends WordSpec with Matchers {

  val fakeApplication = FakeApplication()
  Play.start(fakeApplication)

  implicit val request = FakeRequest()
  implicit var hc = new HeaderCarrier()
  implicit val authContext = Fixtures.buildFakeUser
  val sheetName = "sheetName"

  trait ObservableAuditConnector extends AuditServiceConnector {
    val events: ListBuffer[DataEvent] = new ListBuffer[DataEvent]

    def observedEvents: ListBuffer[DataEvent] = events

    def addEvent(dataEvent: DataEvent): Unit = {
      events += dataEvent
    }

    override def auditData(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Unit = {
      addEvent(dataEvent)
    }
  }

  def createObservableAuditConnector = new ObservableAuditConnector {}

  def createAuditor(observableAuditConnector: ObservableAuditConnector) = {

    val testAuditService = new AuditService {
      override def auditConnector = observableAuditConnector
    }

    new AuditEvents {
      override def auditService: AuditService = testAuditService
    }
  }


  "its should audit runtime errors" in {
    val observableAuditConnector = createObservableAuditConnector
    val auditor = createAuditor(observableAuditConnector)
    var runtimeException : Throwable = null

    try {
      var divideByZero : Int = 0/0
    } catch {
      case e:Throwable => {
        runtimeException = e
        auditor.auditRunTimeError(e, "some context info", sheetName)
      }
    }
  }

  "count the number of rows being checked audit event" in {

    val observableAuditConnector = createObservableAuditConnector
    val auditor = createAuditor(observableAuditConnector)

    auditor.numRowsInSchemeData("sheet", 1)(Fixtures.buildFakeUser,hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))

    observableAuditConnector.events.length should equal(1)

    val event = observableAuditConnector.events.head

    event.auditType should equal("CheckingServiceNumRowsInSchemeData")
    event.detail("sheetName") should equal("sheet")
  }

  "submit audit for a file processing error" in {
    val observableAuditConnector = createObservableAuditConnector
    val auditor = createAuditor(observableAuditConnector)
    val msg = "Could not set the validator"
    auditor.fileProcessingErrorAudit("schemeType", sheetName,msg)(hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))

    observableAuditConnector.events.length should equal(1)

    val event = observableAuditConnector.events.head
    event.auditType should equal("CheckingServiceFileProcessingError")
    event.detail("schemeType") should equal("schemeType")
    event.detail("sheetName") should equal(sheetName)
    event.detail("ErrorMessage") should equal(msg)

  }

}
