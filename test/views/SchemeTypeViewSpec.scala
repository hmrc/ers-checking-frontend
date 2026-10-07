/*
 * Copyright 2026 HM Revenue & Customs
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

package views

import config.ApplicationConfig
import models.CSformMappings.schemeTypeForm
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.scheme_type

class SchemeTypeViewSpec extends ViewSpecBase {

  class SchemeTypeViewSetUp(nonTasRadioEnabled: Boolean, otherRadioDisabled: Boolean) {

    implicit val applicationConfig: ApplicationConfig = new ApplicationConfig(mock[ServicesConfig]) {
      override lazy val displayNonTASSRadio: Boolean = nonTasRadioEnabled
      override lazy val removeOtherRadio: Boolean  = otherRadioDisabled
    }

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
    implicit val messages: Messages                           = testMessages

    val view: scheme_type = app.injector.instanceOf[scheme_type]

    def getRadioButtonsFormDoc(doc: Document): String = doc.getElementsByClass("govuk-radios__item").text()
  }

  "scheme type view" should {
    "show the Non-TASS radio button and Other button when non-tass-radio.enabled is set to true with hint text" in new SchemeTypeViewSetUp(
      true,
      false
    ) {
      val doc: Document = asDocument(view(schemeTypeForm))

      val expectedRadioButtons: String = "Company Share Option Plan (CSOP) " +
        "Enterprise Management Incentives (EMI) " +
        "Save As You Earn (SAYE) " +
        "Share Incentive Plan (SIP) " +
        "Non-tax advantaged share schemes (Non-TASS) For checking files created with sheet names beginning with Non-TASS " +
        "Other For checking files created with sheet names beginning with OTHER"

      getRadioButtonsFormDoc(doc) mustBe expectedRadioButtons
    }

    "not show the Non-TASS radio button when non-tass-radio.enabled is set to false" in new SchemeTypeViewSetUp(
      false,
      false
    ) {
      val doc: Document = asDocument(view(schemeTypeForm))

      val expectedRadioButtons: String = "Company Share Option Plan (CSOP) " +
        "Enterprise Management Incentives (EMI) " +
        "Save As You Earn (SAYE) " +
        "Share Incentive Plan (SIP) " +
        "Other"

      getRadioButtonsFormDoc(doc) mustBe expectedRadioButtons
    }

    "not show the Other radio button post April when other-radio.enabled is set to true" in new SchemeTypeViewSetUp(
      false,
      true
    ) {

      val doc: Document = asDocument(view(schemeTypeForm))

      val expectedRadioButtons: String = "Company Share Option Plan (CSOP) " +
        "Enterprise Management Incentives (EMI) " +
        "Save As You Earn (SAYE) " +
        "Share Incentive Plan (SIP) " +
        "Non-tax advantaged share schemes (Non-TASS) Previously called Other schemes and arrangements"

      getRadioButtonsFormDoc(doc) mustBe expectedRadioButtons
    }
  }

}
