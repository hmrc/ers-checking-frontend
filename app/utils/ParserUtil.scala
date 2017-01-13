/*
 * Copyright 2017 HM Revenue & Customs
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

import models.SheetErrors
import play.api.mvc.{AnyContent, Request}
import play.api.{Play, Logger}
import services.ERSTemplatesInfo
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.collection.mutable.ListBuffer
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

object ParserUtil extends ParserUtil {
  override val cacheUtil: CacheUtil = CacheUtil
}

trait ParserUtil {
  val cacheUtil: CacheUtil

  def formatDataToValidate(rowData: Seq[String], sheetName: String): Seq[String] = {
    val sheetColSize = ERSTemplatesInfo.ersSheets(sheetName.replace(".csv", "")).headerRow.size
    if(rowData.size < sheetColSize) {
      Logger.warn(s"Difference between amount of columns ${rowData.size} and amount of headers ${sheetColSize}")
      val additionalEmptyCells: Seq[String] = Seq.fill(sheetColSize - rowData.size)("")
      (rowData ++ additionalEmptyCells).take(sheetColSize)
    }
    else {
      rowData.take(sheetColSize)
    }
  }

  def isFileValid(errorList: ListBuffer[SheetErrors], source: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Boolean = {
    val isFileValid: Boolean = isValid(errorList)
    if (!isFileValid) {
      cacheUtil.cache[Long](CacheUtil.SCHEME_ERROR_COUNT_CACHE, getTotalErrorCount(errorList)).recover {
        case e: Exception => {
          Logger.error(source + ": Unable to save total scheme error count. Error: " + e.getMessage)
          throw e
        }
      }
      cacheUtil.cache[ListBuffer[SheetErrors]](CacheUtil.ERROR_LIST_CACHE, getSheetErrors(errorList)).recover {
        case e: Exception => {
          Logger.error(source + ": Unable to save error list. Error: " + e.getMessage)
          throw e
        }
      }
    }
    isFileValid
  }

  def isValid(schemeErrors:ListBuffer[SheetErrors]):Boolean = {
    for(sheet <- schemeErrors) {
      for(errors <- sheet.errors){
        return false
      }
    }
    true
  }

  def getTotalErrorCount(schemeErrors: ListBuffer[SheetErrors]): Long = {
    var totalErrors = 0
    if(totalErrors != schemeErrors.size)
      for(i <- 0 to schemeErrors.size-1) totalErrors += schemeErrors(i).errors.length
    totalErrors
  }


  def getSheetErrors(schemeErrors: ListBuffer[SheetErrors]): ListBuffer[SheetErrors] = {
    val errorCount: Int = Play.configuration.getInt(s"errorDisplayCount").getOrElse(100)
    schemeErrors.map { schemeError =>
      SheetErrors(schemeError.sheetName,schemeError.errors.take(errorCount))
    }
  }
  
}
