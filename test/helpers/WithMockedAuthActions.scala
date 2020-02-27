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

package helpers

import controllers.auth.{AuthAction, RequestWithOptionalEmpRef}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.{Answer, OngoingStubbing}
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future

trait WithMockedAuthActions {

  val mockAuthAction: AuthAction

  def mockAnyContentAction: OngoingStubbing[Action[AnyContent]] =
    when(mockAuthAction.async(ArgumentMatchers.any[RequestWithOptionalEmpRef[AnyContent] => Future[Result]]()))
      .thenAnswer(new Answer[Action[AnyContent]] {
        override def answer(invocation: InvocationOnMock): Action[AnyContent] = {
          val passedInBlock = invocation.getArguments()(0).asInstanceOf[RequestWithOptionalEmpRef[AnyContent] => Future[Result]]
          new Action[AnyContent]{
            override def parser: BodyParser[AnyContent] = BodyParsers.parse.default
            override def apply(request: Request[AnyContent]): Future[Result] = passedInBlock(RequestWithOptionalEmpRef(request, None))
          }
        }
      })
}
