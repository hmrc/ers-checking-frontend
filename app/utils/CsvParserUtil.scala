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

import akka.NotUsed
import akka.stream.scaladsl.Flow
import config.ApplicationConfig
import javax.inject.{Inject, Singleton}
import models.SheetErrors
import models.upscan.UpscanCsvFilesCallback
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import services.ERSTemplatesInfo
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class CsvParserUtil @Inject()(appConfig: ApplicationConfig
                          )(implicit ec: ExecutionContext) {

  def formatDataToValidate(rowData: Seq[String], sheetName: String): Seq[String] = {
    val sheetColSize = ERSTemplatesInfo.ersSheets(sheetName.replace(".csv", "")).headerRow.length
    /*
    if (rowData.length < sheetColSize) {
      Logger.debug(s"Difference between amount of columns ${rowData.size} and amount of headers $sheetColSize")
      val additionalEmptyCells: Seq[String] = Seq.fill(sheetColSize - rowData.size)("")
      (rowData ++ additionalEmptyCells).take(sheetColSize)
    }
    else {

     */
      rowData.take(sheetColSize)

  }

  def getSheetErrors(schemeErrors: SheetErrors): SheetErrors = {
    schemeErrors.copy(errors = schemeErrors.errors.take(appConfig.errorCount))
  }

}