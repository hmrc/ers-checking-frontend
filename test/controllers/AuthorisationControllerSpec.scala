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

package controllers

import config.ApplicationConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.mvc.MessagesControllerComponents
import play.api.test.{FakeRequest, Injecting}
import play.mvc.Http.Status
import uk.gov.hmrc.play.test.UnitSpec
import views.html.not_authorised

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AuthorisationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with Injecting {
  val controllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val view: not_authorised = inject[not_authorised]

  val authController: AuthorisationController = new AuthorisationController(controllerComponents, mockAppConfig, view)

  "AuthorisationController" should {
    "call notAuthorised" in {
      val result = Await.result(authController.notAuthorised.apply(FakeRequest()), Duration.Inf)

      result.header.status shouldBe Status.UNAUTHORIZED
      assert(result.body.consumeData.utf8String.contains("You aren’t authorised to access ERS checking service"))
    }
  }
}
