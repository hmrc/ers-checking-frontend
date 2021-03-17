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

package services.validation
import play.api.i18n.Messages
import uk.gov.hmrc.services.validation.models.ValidationError


trait ValidationErrorHelper[A] {
  def withErrorsFromMessages(a: A)(implicit messages: Messages): Option[List[ValidationError]]
}

object ValidationErrorHelper {
  def apply[A](implicit sh: ValidationErrorHelper[A]): ValidationErrorHelper[A] = sh

  implicit class ErrorFromMessagesOps[A: ValidationErrorHelper](a: A)(implicit messages: Messages) {
    def withErrorsFromMessages: Option[List[ValidationError]] = ValidationErrorHelper[A].withErrorsFromMessages(a)
  }

  implicit val convertMessages: ValidationErrorHelper[Option[List[ValidationError]]] = {
    new ValidationErrorHelper[Option[List[ValidationError]]] {
      def withErrorsFromMessages(veListOpt: Option[List[ValidationError]])(implicit messages: Messages): Option[List[ValidationError]] = {
        veListOpt match {
          case Some(veList) => Some(veList.map(ve => ve.copy(errorMsg = Messages(ve.errorMsg))))
          case None => None
        }
      }
    }
  }
}