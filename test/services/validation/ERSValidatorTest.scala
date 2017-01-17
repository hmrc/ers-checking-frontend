/*
 * Copyright 2017 HM Revenue & Customs
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

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import services.validation.EMITestData.ERSValidationEMIAdjustmentsTestData
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.services.validation.DataValidator


class ERSValidatorTest extends PlaySpec with OneServerPerSuite with ScalaFutures with MockitoSugar with ERSValidationEMIAdjustmentsTestData {

  val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

  implicit val hc = HeaderCarrier()

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-emi-adjustments-validation-config"))

  val testData =  Seq("yes", "yes", "yes", "4", "2011-10-13", "Mia", "Iam", "Aim", "AB123456C", "123/XZ55555555", "10.1234", "10.14", "10.1324", "10.1244")
  "ERSValidator" should {
    "should return valid cells" in {
      ErsValidator.getCells(testData,1) mustBe getValidRowData
    }

    "pass a row of valid EMI adjustments data without failure" in {
      ErsValidator.validateRow(getValidRowData.map(_.value),10,validator) mustBe None
    }
    "pass a row of invalid EMI adjustments data with failure" in {
      ErsValidator.validateRow(getInvalidRowData.map(_.value),10,validator).get.size mustBe 14
    }
  }

}
