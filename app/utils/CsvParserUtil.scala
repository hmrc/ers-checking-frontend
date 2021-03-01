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
class CsvParserUtil @Inject()(val ersUtil: ERSUtil,
                              appConfig: ApplicationConfig
                          )(implicit ec: ExecutionContext) {
  val HUNDRED = 100

  def formatDataToValidate(rowData: Seq[String], sheetName: String): Seq[String] = {
    val sheetColSize = ERSTemplatesInfo.ersSheets(sheetName.replace(".csv", "")).headerRow.size
    if(rowData.size < sheetColSize) {
      Logger.debug(s"Difference between amount of columns ${rowData.size} and amount of headers $sheetColSize")
      val additionalEmptyCells: Seq[String] = Seq.fill(sheetColSize - rowData.size)("")
      (rowData ++ additionalEmptyCells).take(sheetColSize)
    }
    else {
      rowData.take(sheetColSize)
    }
  }

  def isFileValid(sheetErrors: SheetErrors, file: Option[UpscanCsvFilesCallback] = None)
								 (implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Try[Boolean]] = {
    if (sheetErrors.errors.isEmpty) {
      Future.successful(Success(true))
    }
    else {
			val updatedErrorCount = sheetErrors.errors.length
			val updatedErrorList = getSheetErrors(sheetErrors)
      val id = file.map(_.uploadId).getOrElse("")

      val result = for {
				_ <- ersUtil.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}$id", updatedErrorCount)
				_ <- ersUtil.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}$id",
          new ListBuffer[SheetErrors]() :+ updatedErrorList)
			} yield Success(false)

      result recover {
        case ex: Exception => Failure(ex)
      }
    }
  }

  def getSheetErrors(schemeErrors: SheetErrors): SheetErrors = {
    val errorCount: Int = appConfig.errorCount.getOrElse(HUNDRED)
    SheetErrors(schemeErrors.sheetName, schemeErrors.errors.take(errorCount))
  }

  def getHeadersForSchema(schema: String): Seq[String] = schema match {
    case ""
  }

}
