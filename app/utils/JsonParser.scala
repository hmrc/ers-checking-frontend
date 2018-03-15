/*
 * Copyright 2018 HM Revenue & Customs
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

import models._
import play.api.libs.json.{JsValue, Json}

object JsonParser extends JsonParser
trait  JsonParser {

  def parseErrorList(errorList: String): ErrorListJSON = {
    val errorListJsValue: JsValue = Json.parse(errorList)
    val totalErrorCount: String = (errorListJsValue \ "1").as[String] // total error count
    val errorListJson: ErrorListJSON = new ErrorListJSON(totalErrorCount)

    val sheets: Seq[JsValue] = (errorListJsValue \ "3").as[Seq[JsValue]] // sheets array

    for (sheet <- sheets) {

      val sheetNumber: String = (sheet \ "4").as[String] // sheet number
      val sheetName: String = (sheet \ "5").as[String] // sheet name
      val errorCount: String = (sheet \ "6").as[String] // sheet error count

      val sheetJSON: SheetJSON = new SheetJSON(sheetNumber, sheetName, errorCount)

      val errors: Seq[JsValue] = (sheet \ "7").as[Seq[JsValue]] // errors array
      for (error <- errors) {
        val cellColumn: String = (error \ "8").as[String]; // column
        val cellRow: String = (error \ "9").as[String]; // row
        val cellError: String = (error \ "a").as[String]; // error code
        val cellErrorJson: CellErrorJSON = new CellErrorJSON(cellColumn, cellRow, cellError);
        sheetJSON.addError(cellErrorJson)
      }
      errorListJson.addSheet(sheetJSON);
    }
    errorListJson
  }

  def parseCorrelatedData(correlatedData: String): CorrelatedDataJSON = {
    val correlatedDataJSON = new CorrelatedDataJSON

    val json = Json.parse(correlatedData)

    val errorTypeCounts = (json \ "b").as[Seq[JsValue]] // correlated data array

    for(errorTypeCount <- errorTypeCounts){
      val errorCode: String = (errorTypeCount \ "c").as[String] // error code
      val errorCount: String = (errorTypeCount \ "d").as[String] // error count

      val errorTypeCountJSON = new ErrorTypeCountJSON(errorCode, errorCount)
      correlatedDataJSON.putCorrelatedData(errorTypeCountJSON)
    }
    correlatedDataJSON
  }

}
