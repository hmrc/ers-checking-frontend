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

import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import services.validation.EMITestData.ERSValidationEMIAdjustmentsTestData
import uk.gov.hmrc.services.validation.DataValidator

class ERSValidatorTest extends PlaySpec with GuiceOneServerPerTest with MockitoSugar with ERSValidationEMIAdjustmentsTestData {
  // scalastyle:off magic.number

  val validator: DataValidator = new DataValidator(ConfigFactory.load.getConfig("ers-emi-adjustments-validation-config"))
  val ersValidator: ErsValidator = new ErsValidator

  val testData =  Seq("yes", "yes", "yes", "4", "2011-10-13", "Mia", "Iam", "Aim", "AB123456C", "123/XZ55555555", "10.1234", "10.14", "10.1324", "10.1244")
  "ERSValidator" should {
    "should return valid cells" in {
      ersValidator.getCells(testData,1) mustBe getValidRowData
    }

    "pass a row of valid EMI adjustments data without failure" in {
      ersValidator.validateRow(validator)(getValidRowData.map(_.value),10) mustBe None
    }
    "pass a row of invalid EMI adjustments data with failure" in {
      ersValidator.validateRow(validator)(getInvalidRowData.map(_.value),10).get.size mustBe 14
    }

    "throw exception if validator throws exception" in {
      val mockValidator = mock[DataValidator]
      val exception = new RuntimeException("this is not good")
      when(mockValidator.validateRow(any())).thenThrow(exception)
      val thrown = intercept[RuntimeException] {
        ersValidator.validateRow(mockValidator)(getInvalidRowData.map(_.value),10)
      }
      thrown mustBe a[RuntimeException]
      thrown.getMessage mustBe "this is not good"
    }
  }

}
