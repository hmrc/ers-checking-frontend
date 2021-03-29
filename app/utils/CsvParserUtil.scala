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

package utils

import config.ApplicationConfig
import models.SheetErrors
import services.ERSTemplatesInfo

import javax.inject.{Inject, Singleton}

@Singleton
class CsvParserUtil @Inject()(appConfig: ApplicationConfig) {

  def formatDataToValidate(rowData: Seq[String], sheetName: String): Seq[String] = {
    val sheetColSize = ERSTemplatesInfo.ersSheets(sheetName.replace(".csv", "")).headerRow.length
    rowData.take(sheetColSize).map(_.trim)
  }

  def getSheetErrors(schemeErrors: SheetErrors): SheetErrors = {
    schemeErrors.copy(errors = schemeErrors.errors.take(appConfig.errorCount))
  }

  def rowIsEmpty(row: List[String]): Boolean = {
    if (row.length > 1) {
      false
    } else if (row.isEmpty) {
      true
    } else {
      row.headOption.getOrElse("").trim.isEmpty
    }
  }
}