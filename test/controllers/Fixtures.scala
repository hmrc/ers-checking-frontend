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

import controllers.auth.RequestWithOptionalEmpRef
import models.SheetErrors
import play.api.libs.json._
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import uk.gov.hmrc.services.validation.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer

object Fixtures {

  def buildFakeRequestWithSessionId(method: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "").withSession("sessionId" -> "FAKE_SESSION_ID")

  def buildEmpRefRequestWithSessionId(method: String): RequestWithOptionalEmpRef[AnyContent] =
    RequestWithOptionalEmpRef(FakeRequest(method, "").withSession("sessionId" -> "FAKE_SESSION_ID"), None)

  def buildSheetErrors: ListBuffer[SheetErrors] =  {
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

    schemeErrors
  }

  def getMockErrorList: JsValue = {
    val schemeErrors = buildSheetErrors
    val jsonValue: JsValue = Json.toJson(schemeErrors);
    jsonValue;
  }

  def getMockSummaryErrors: JsValue = {
    val errorList: String = "\"{\\\"b\\\":[{\\\"c\\\":\\\"11\\\", \\\"d\\\":\\\"1\\\"}]}\"";
    val jsonValue: JsValue = Json.parse(errorList);
    jsonValue;
  }

  def getMockSchemeTypeString: String = "1"
}