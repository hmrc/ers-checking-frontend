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

package models

import java.io.File
import java.nio.file.Path

import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.services.validation._

import scala.collection.mutable.ListBuffer

case class FileInfo(errorMessage: String, fileName: String, fileID: String, fileType: String)

case class ERSFileProcessingException(message: String,
                                      context: String,
                                      jsonSize: Option[Int] = None,
                                      needsExtendedInstructions: Boolean = false,
                                      optionalParams: Seq[String] = Nil) extends Exception(message)

case class CallbackData(collection: String, id: String, length: Long, name: Option[String], contentType: Option[String], customMetadata: Option[JsObject])

object CallbackData {
  implicit val format: OFormat[CallbackData] = Json.format[CallbackData]
}

case class SheetErrors (sheetName: String, errors: ListBuffer[ValidationError])
object SheetErrors {
  implicit val formatCell: OFormat[Cell] = Json.format[Cell]
  implicit val formatErrors: OFormat[ValidationError] = Json.format[ValidationError]
  implicit val format: OFormat[SheetErrors] = Json.format[SheetErrors]
}

case class FileObject(fileName: String, file: Path)
