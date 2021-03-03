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

package services

import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.services.validation.ValidationError
import utils.CsvParserUtil

import scala.concurrent.ExecutionContext

@Singleton
class CsvFileProcessor @Inject()(parserUtil: CsvParserUtil,
                                )(implicit executionContext: ExecutionContext) {

  type RowValidator = (Seq[String], Int) => Option[List[ValidationError]]

  def processRow(rowBytes: List[ByteString], sheetName: String, validator: RowValidator): List[ValidationError] = {
      val rowStrings = rowBytes.map(byteString => byteString.utf8String)
      val parsedRow = parserUtil.formatDataToValidate(rowStrings, sheetName)
//    Logger.info("parserRow is " + parsedRow)
      validator(parsedRow, 0) match {
        case Some(newErrors) if newErrors.nonEmpty =>
//          Logger.info("[CsvFileProcessor][processChunk] schemeErrors size is " + newErrors.size + newErrors)
          newErrors
        case _ => List.empty
      }
  }

}
