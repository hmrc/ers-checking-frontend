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
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.api.test.{FakeRequest, Injecting}
import play.mvc.Http.Status
import views.html.not_authorised

class AuthorisationControllerSpec extends AnyWordSpecLike with Matchers with OptionValues with GuiceOneAppPerSuite
  with MockitoSugar with Injecting with ScalaFutures with ErsTestHelper {

  private val mcc = testMCC(app)
  val view: not_authorised = inject[not_authorised]
  val authController: AuthorisationController = new AuthorisationController(mcc, mockAppConfig, view)

  "AuthorisationController" should {
    "call notAuthorised" in {
      val result = authController.notAuthorised.apply(FakeRequest())

      result.futureValue.header.status shouldBe Status.UNAUTHORIZED
      assert(contentAsString(result) contains "You aren’t authorised to access ERS checking service")
    }
  }
}
