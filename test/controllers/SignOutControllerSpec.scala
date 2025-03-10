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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout, status, stubMessages}
import play.api.test.{FakeRequest, Injecting}
import views.html.signed_out

import scala.concurrent.Future

class SignOutControllerSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite
   with Injecting with ErsTestHelper {

  implicit val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]
  implicit val timeoutView: signed_out = inject[signed_out]
  val signOutController = new SignOutController(mcc, timeoutView)

  "Calling SignOutController.timeout" should {
    val result: Future[Result] = signOutController.timedOut().apply(FakeRequest())

    "have a status of Ok" in {
      val result = signOutController.timedOut().apply(FakeRequest())
      status(result) must be(OK)
    }

    "have a title of Check your Employment Related Securities" in {
      val result = signOutController.timedOut().apply(FakeRequest())
      contentAsString(result) must include("Check your Employment Related Securities (ERS) files")
    }

    "have some text on the page" in {
      val result = signOutController.timedOut().apply(FakeRequest())
      contentAsString(result) must include("For your security, we signed you out")
    }

    "render the timeout page" in {
      def returnMessage(key: String): String = stubMessages(mcc.messagesApi).messages(key)

      val title: String = returnMessage("ers.header")
      val header: String = returnMessage("ers_signed_out.info")
      val signInButton: String = returnMessage("ers_signed_out.link")
      val pageAsString: String = contentAsString(result)

      pageAsString must include(title)
      pageAsString must include(header)
      pageAsString must include(signInButton)
    }
  }
}
