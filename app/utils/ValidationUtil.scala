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

package utils

import uk.gov.hmrc.validator.models.ods.SheetErrors

import scala.collection.mutable.ListBuffer

object ValidationUtil {

  def isValid(schemeErrors: ListBuffer[SheetErrors]): Boolean = {
    schemeErrors.map(_.errors.isEmpty).forall(identity)
  }

  def getSheetErrors(schemeErrors: ListBuffer[SheetErrors], errorCount: Int): ListBuffer[SheetErrors] = {
    schemeErrors.map { schemeError =>
      SheetErrors(schemeError.sheetName, schemeError.errors.take(errorCount))
    }
  }

}
