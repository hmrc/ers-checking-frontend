/*
 * Copyright 2025 HM Revenue & Customs
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

import helpers.ErsTestHelper
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.MessagesImpl
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import views.html.{global_error, not_enrolled_in_paye, sign_out_paye}

class CheckPAYEControllerSpec extends AnyWordSpecLike with Matchers with OptionValues
  with GuiceOneAppPerSuite with ErsTestHelper with Injecting with ScalaFutures {

  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication())
  val notEnrolledView: not_enrolled_in_paye = inject[not_enrolled_in_paye]
  val signOutView: sign_out_paye = inject[sign_out_paye]
  val globalErrorView: global_error = inject[global_error]

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  def buildFakeCheckPAYEController() = new CheckPAYEController(mockAuthAction, mcc, notEnrolledView, signOutView, globalErrorView)(mockAppConfig) {
    mockAnyContentAction
  }

  "check status for missingPAYE" should {
    "return 200 OK and render the not_enrolled_in_paye view" in {
      val controllerUnderTest = buildFakeCheckPAYEController()
      val result = controllerUnderTest.missingPAYE().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe OK
    }

    "checking page for missingPAYE" in {
      val controllerUnderTest = buildFakeCheckPAYEController()
      val result = controllerUnderTest.showMissingPAYE()(Fixtures.buildEmpRefRequestWithSessionId("GET", mockAppConfig), testMessages)
      status(result) shouldBe OK

      contentAsString(result) should include("There is a problem – Employment Related Securities – GOV.UK")
      contentAsString(result) should include("There is a problem")

    }
  }
  "check status for signOutPAYE" should {
    "return 200 OK and render the sign_out_paye view" in {
      val controllerUnderTest = buildFakeCheckPAYEController()
      val result = controllerUnderTest.signOutPAYE().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe OK
    }

    "checking page for signOutPAYE" in {
      val controllerUnderTest = buildFakeCheckPAYEController()
      val result = controllerUnderTest.showSignOutPAYE()(Fixtures.buildEmpRefRequestWithSessionId("GET", mockAppConfig), testMessages)
      status(result) shouldBe OK
      contentAsString(result) should include("You have signed out – Employment Related Securities – GOV.UK")
      contentAsString(result) should include("You have signed out")

    }

  }


}
