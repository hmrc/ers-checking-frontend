/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.auth

import akka.stream.Materializer
import controllers.routes
import helpers.ErsTestHelper
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{Assertion, BeforeAndAfterEach}
import play.api.Play
import play.api.http.Status
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class AuthActionSpec extends UnitSpec with ErsTestHelper with BeforeAndAfterEach with WithFakeApplication {

  override val mockAuthConnector: AuthConnector = mock[AuthConnector]
  override val testBodyParser: BodyParsers.Default = fakeApplication.injector.instanceOf[BodyParsers.Default]
  implicit def materializer: Materializer = Play.materializer(fakeApplication)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }

  def authAction: AuthAction = new AuthAction(mockAuthConnector, mockAppConfig, testBodyParser)

  def defaultAsyncBody(requestTestCase: RequestWithOptionalEmpRef[_] => Assertion): RequestWithOptionalEmpRef[_] => Result = testRequest => {
    requestTestCase(testRequest)
    Results.Ok("Successful")
  }

  def getPredicate: Predicate =
    AuthProviders(GovernmentGateway) and ConfidenceLevel.L50

  val ersEnrolments: Enrolments =
    Enrolments(Set(
      Enrolment("IR-PAYE",
        Seq(EnrolmentIdentifier("TaxOfficeNumber","1234"), EnrolmentIdentifier("TaxOfficeReference","1234")),
        "Activated",
        None))
    )

  val nonePayeEnrolments: Enrolments =
    Enrolments(Set(
      Enrolment("test1", Seq(EnrolmentIdentifier("Dummy","1234")), "Activated", None),
      Enrolment("IR-PAYE", Seq(EnrolmentIdentifier("TaxOfficeNumber","1234")), "Activated", None))
    )

  "AuthAction" should {
    "return a perform the action if the user is authorised with an empref in the request" in {
      when(
        mockAuthConnector
          .authorise(
            ArgumentMatchers.eq(getPredicate),
            ArgumentMatchers.eq(allEnrolments)
          )(
            ArgumentMatchers.any(), ArgumentMatchers.any()
          )
      ).thenReturn(Future.successful(ersEnrolments))

      val result: Future[Result] = authAction(
        defaultAsyncBody(_.optionalEmpRef shouldBe Some(EmpRef("1234", "1234")))
      )(FakeRequest())
      status(result) shouldBe Status.OK
      await(
        bodyOf(result).map(
          _ shouldBe "Successful"
        )
      )
    }


    "return a perform the action if the user is authorised without and empref when user has multiple enrolments" in {
      when(
        mockAuthConnector
          .authorise(
            ArgumentMatchers.eq(getPredicate),
            ArgumentMatchers.eq(allEnrolments)
          )(
            ArgumentMatchers.any(), ArgumentMatchers.any()
          )
      ).thenReturn(Future.successful(nonePayeEnrolments))

      val result: Future[Result] = authAction(
        defaultAsyncBody(_.optionalEmpRef shouldBe None)
      )(FakeRequest())
      status(result) shouldBe Status.OK
      await(
        bodyOf(result).map(
          _ shouldBe "Successful"
        )
      )
    }

    "return a perform the action if the user is authorised without and empref when user has no enrolments" in {
      when(
        mockAuthConnector
          .authorise(
            ArgumentMatchers.eq(getPredicate),
            ArgumentMatchers.eq(allEnrolments)
          )(
            ArgumentMatchers.any(), ArgumentMatchers.any()
          )
      ).thenReturn(Future.successful(Enrolments(Set())))

      val result: Future[Result] = authAction(
        defaultAsyncBody(_.optionalEmpRef shouldBe None)
      )(FakeRequest())
      status(result) shouldBe Status.OK
      await(
        bodyOf(result).map(
          _ shouldBe "Successful"
        )
      )
    }

    "return a 401 if an SessionRecordNotFound Exception (NoActiveSession) is experienced" in {
      when(
        mockAuthConnector
          .authorise(
            ArgumentMatchers.eq(getPredicate),
            ArgumentMatchers.eq(allEnrolments)
          )(
            ArgumentMatchers.any(), ArgumentMatchers.any()
          )
      ).thenReturn(Future.failed(SessionRecordNotFound("failed")))

      val result: Future[Result] = authAction(defaultAsyncBody(_.optionalEmpRef shouldBe None))(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "http://localhost:9553/bas-gateway/sign-in?" +
        "continue_url=http%3A%2F%2Flocalhost%3A9225%2Fcheck-your-ers-files&origin=ers-checking-frontend"
    }

    "return a 401 if an UnsupportedAuthProvider Exception is experienced" in {
      when(
        mockAuthConnector
          .authorise(
            ArgumentMatchers.eq(getPredicate),
            ArgumentMatchers.eq(allEnrolments)
          )(
            ArgumentMatchers.any(), ArgumentMatchers.any()
          )
      ).thenReturn(Future.failed(UnsupportedAuthProvider("failed")))

      val result: Future[Result] = authAction(defaultAsyncBody(_.optionalEmpRef shouldBe None))(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.AuthorisationController.notAuthorised().url
    }

    "return a 401 if an InsufficientConfidenceLevel Exception is experienced" in {
      when(
        mockAuthConnector
          .authorise(
            ArgumentMatchers.eq(getPredicate),
            ArgumentMatchers.eq(allEnrolments)
          )(
            ArgumentMatchers.any(), ArgumentMatchers.any()
          )
      ).thenReturn(Future.failed(InsufficientConfidenceLevel("failed")))

      val result: Future[Result] = authAction(defaultAsyncBody(_.optionalEmpRef shouldBe None))(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.AuthorisationController.notAuthorised().url
    }
  }

}