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

package controllers

import helpers.ErsTestHelper
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import play.api.test.{FakeRequest, Injecting}
import play.mvc.Http.Status
import views.html.{individual_not_authorised, individual_signout, not_authorised}

class AuthorisationControllerSpec extends AnyWordSpecLike with Matchers with OptionValues with GuiceOneAppPerSuite
  with MockitoSugar with Injecting with ScalaFutures with ErsTestHelper {

  private val mcc = testMCC(app)
  val view: not_authorised = inject[not_authorised]
  val individual_not_authorised_view: individual_not_authorised = inject[individual_not_authorised]
  val individual_signout_view: individual_signout = inject[individual_signout]
  val authController: AuthorisationController = new AuthorisationController(mcc, mockAppConfig, view, individual_not_authorised_view, individual_signout_view)

  "AuthorisationController" should {
    "call notAuthorised" in {
      val result = authController.notAuthorised.apply(FakeRequest())
      result.futureValue.header.status shouldBe Status.UNAUTHORIZED
      assert(contentAsString(result) contains "You aren’t authorised to access ERS checking service")
    }

    "call individualNotAuthorised" in {
      val result = authController.individualNotAuthorised.apply(FakeRequest())
      result.futureValue.header.status shouldBe Status.UNAUTHORIZED
      assert(contentAsString(result) contains "You signed in using a Government Gateway user ID for an individual.")
    }


    "call individualSignout" in {
      val result = authController.individualSignout.apply(FakeRequest())
      result.futureValue.header.status shouldBe Status.OK
      assert(contentAsString(result) contains
        "To check your Employment Related Securities (ERS) file you will need to sign in with the Government Gateway user ID " +
          "and password that you use to manage PAYE for employers in your business tax account.")
    }


    "check url for individualSignoutRedirect for POST" in {
      val result = authController.individualSignoutRedirect().apply(FakeRequest())
      result.futureValue.header.status shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.AuthorisationController.individualSignout().url)
    }

  }
}