/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.services.validation.{Cell, DataValidator, ValidationError}
import models.ValidationErrorData
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.i18n.Messages.Implicits._
import services.validation.ValidationErrorHelper._

/**
 * Created by matt on 25/01/16.
 */
trait ValidationTestRunner extends PlaySpec with GuiceOneAppPerSuite{

  def injector: Injector = app.injector
  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
  override def fakeApplication() = new GuiceApplicationBuilder().configure(Map("play.i18n.langs"->List("en", "cy"))).build()


  def populateValidationError(expRes: ValidationErrorData)(implicit cell: Cell) = {
    ValidationError(cell, expRes.id, expRes.errorId, expRes.errorMsg)
  }

  def resultBuilder(cellData: Cell, expectedResultsMaybe: Option[List[ValidationErrorData]]): Option[List[ValidationError]] = {
    if (expectedResultsMaybe.isDefined) {
      implicit val cell: Cell = cellData
      val validationErrors = expectedResultsMaybe.get.map(errorData => populateValidationError(errorData))
      Some(validationErrors)
    } else None
  }

  def runTests(validator:DataValidator, descriptions: List[String], testDatas:List[Cell], expectedResults:List[Option[List[ValidationErrorData]]]) = {
      for (x <- 0 until descriptions.length) {
        descriptions(x) in {
          validator.validateCell(testDatas(x), Some(ValidationContext)).withErrorsFromMessages mustBe resultBuilder(testDatas(x), expectedResults(x))
        }
      }
  }
}
