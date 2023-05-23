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

package services.validation

import helpers.ErsTestHelper
import models.ValidationErrorData
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesImpl
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.DefaultMessagesControllerComponents
import play.api.{Application, i18n}
import services.validation.ValidationErrorHelper._
import uk.gov.hmrc.services.validation.DataValidator
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}

trait ValidationTestRunner extends PlaySpec with GuiceOneAppPerSuite with ErsTestHelper {
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApp())

  def fakeApp(): Application = new GuiceApplicationBuilder().configure(Map("play.i18n.langs" -> List("en", "cy"), "metrics.enabled" -> "false")).build()

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  def runValidationTests(validator: DataValidator,
                         descriptions: List[String],
                         testDatas: List[Cell],
                         expectedResults: List[Option[List[ValidationErrorData]]]): Unit = {
    for (x <- descriptions.indices) {
      descriptions(x) in {
        val validatedCell = validator.validateCell(testDatas(x)) match {
          case Some(x) => Some(List(x))
          case None => Option.empty[List[ValidationError]]
        }
        validatedCell.withErrorsFromMessages mustBe resultBuilder(testDatas(x), expectedResults(x))
      }
    }
  }

  def resultBuilder(cellData: Cell, expectedResultsMaybe: Option[List[ValidationErrorData]]): Option[List[ValidationError]] = {
    if (expectedResultsMaybe.isDefined) {
      implicit val cell: Cell = cellData
      val validationErrors = expectedResultsMaybe.get.map(errorData => populateValidationError(errorData))
      Some(validationErrors)
    } else {
      None
    }
  }

  def populateValidationError(expRes: ValidationErrorData)(implicit cell: Cell): ValidationError = {
    ValidationError(cell, expRes.id, expRes.errorId, expRes.errorMsg)
  }
}
