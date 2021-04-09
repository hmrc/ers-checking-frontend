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

package utils

import helpers.ErsTestHelper
import models.SheetErrors
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.MessagesImpl
import play.api.mvc.DefaultMessagesControllerComponents
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer

class HtmlCreatorSpec extends UnitSpec with ErsTestHelper with GuiceOneAppPerSuite {

  lazy val mcc: DefaultMessagesControllerComponents = testMCC(app)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  "HtmlCreator" should {
    "parse non-empty sheetErrors correctly" in {
      val validationError: ValidationError = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
      val testSheetErrors: SheetErrors = SheetErrors("aName", ListBuffer(validationError))

      val htmlCreator = new HtmlCreator

      htmlCreator.getSheets(ListBuffer(testSheetErrors)) shouldBe "<h3 class=sheet-title><span class=font-small>aName</span></h3><table id=aName><tr><th class=column scope=col>Column</th><th class=row scope=col>Row</th><th class=errorTitle scope=col>Error</th></tr><tr><td>A</td><td>1</td><td class=errorMsg>This entry must be 'yes' or 'no'.</td></tr></table>"
    }
    "parse empty sheetErrors correctly" in {
      val testSheetErrors: SheetErrors = SheetErrors("aName", ListBuffer())

      val htmlCreator = new HtmlCreator

      htmlCreator.getSheets(ListBuffer(testSheetErrors)) shouldBe ""
    }
  }
}
