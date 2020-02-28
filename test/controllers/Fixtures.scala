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

import controllers.auth.RequestWithOptionalEmpRef
import models.SheetErrors
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.test.WithApplication
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L0
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, CredentialStrength}
import uk.gov.hmrc.services.validation.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import uk.gov.hmrc.http.HeaderCarrier

object Fixtures {

  def buildFakeAuthority = Authority("/auth/oid/krogers", Accounts(None), None, None, CredentialStrength.Strong, L0, None, None, None, "")
  def buildFakeUser = AuthContext(buildFakeAuthority)

  def buildFakeRequestWithSessionId(method: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "").withSession("sessionId" -> "FAKE_SESSION_ID")

  def buildEmpRefRequestWithSessionId(method: String): RequestWithOptionalEmpRef[AnyContent] =
    RequestWithOptionalEmpRef(FakeRequest(method, "").withSession("sessionId" -> "FAKE_SESSION_ID"), None)

  def getMockErrorList(): JsValue = {
    val schemeErrors = new ListBuffer[SheetErrors]()
    val list1 = ValidationError(Cell("A",1,"abc"),"001", "error.1", "This entry must be 'yes' or 'no'.")
    val list2 = ValidationError(Cell("B",1,"abc"),"001", "error.1", "This entry must be 'yes' or 'no'.")
    val list3 = ValidationError(Cell("C",1,"abc"),"001", "error.1", "This entry must be 'yes' or 'no'.")
    val sheetErrors3 = new ListBuffer[ValidationError]()
    sheetErrors3 += list1
    sheetErrors3 += list2
    sheetErrors3 += list3
    schemeErrors += SheetErrors("Sheet1",sheetErrors3)
    schemeErrors += SheetErrors("Sheet2",sheetErrors3)
    val sheetErrors1 = new ListBuffer[ValidationError]()
    sheetErrors1 += list1
    schemeErrors += SheetErrors("Sheet3",sheetErrors1)

    val errorList: String = "\"{\\\"1\\\":\\\"0\\\",\\\"2\\\":\\\"\\\",\\\"3\\\":[{\\\"4\\\":\\\"2\\\",\\\"5\\\":\\\"herokio\\\",\\\"6\\\":\\\"1\\\",\\\"7\\\":[{\\\"8\\\":\\\"7\\\",\\\"9\\\":\\\"14\\\",\\\"a\\\":\\\"11\\\"}]}]}\""
    val jsonValue: JsValue = Json.toJson(schemeErrors);
    jsonValue;
  }

//  def getMockErrorList(): JsValue = {
//    val errorList: String = "\"{\\\"1\\\":\\\"0\\\",\\\"2\\\":\\\"\\\",\\\"3\\\":[{\\\"4\\\":\\\"2\\\",\\\"5\\\":\\\"herokio\\\",\\\"6\\\":\\\"1\\\",\\\"7\\\":[{\\\"8\\\":\\\"7\\\",\\\"9\\\":\\\"14\\\",\\\"a\\\":\\\"11\\\"}]}]}\""
//    val jsonValue: JsValue = Json.parse(errorList);
//    jsonValue;
//  }

  def getMockSummaryErrors(): JsValue = {
    val errorList: String = "\"{\\\"b\\\":[{\\\"c\\\":\\\"11\\\", \\\"d\\\":\\\"1\\\"}]}\"";
    val jsonValue: JsValue = Json.parse(errorList);
    jsonValue;
  }

  def getMockSchemeTypeString: String = "1"

  def getAwaitDuration(): Duration = {
    60 seconds
  }

}


abstract class WithErsSetup extends WithApplication/*(FakeApplication(additionalConfiguration = Map(
  "application.secret" -> "test",
  "govuk-tax.Test.login-callback.url" -> "test"
))) */{
  implicit val hc = HeaderCarrier()
}

trait ERSFakeApplication extends BeforeAndAfterAll {
  this: Suite =>


  val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

   lazy val fakeApplication = new GuiceApplicationBuilder().configure(config).build()
}
