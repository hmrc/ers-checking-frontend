/*
 * Copyright 2022 HM Revenue & Customs
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

package models.upscan

import play.api.libs.json._

sealed trait UploadStatus
case object NotStarted extends UploadStatus
case object InProgress extends UploadStatus
case object Failed extends UploadStatus
case object FailedMimeType extends UploadStatus

case class UploadedSuccessfully(name: String, downloadUrl: String, noOfRows: Option[Int] = None) extends UploadStatus

object UploadedSuccessfully {
  implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]
}

object UploadStatus {
  implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]
  implicit val readsUploadStatus: Reads[UploadStatus] = (json: JsValue) => {
    val jsObject = json.asInstanceOf[JsObject]
    jsObject.value.get("_type") match {
      case Some(JsString("NotStarted")) => JsSuccess(NotStarted)
      case Some(JsString("InProgress")) => JsSuccess(InProgress)
      case Some(JsString("Failed")) => JsSuccess(Failed)
      case Some(JsString("FailedMimeType")) => JsSuccess(FailedMimeType)
      case Some(JsString("UploadedSuccessfully")) => Json.fromJson[UploadedSuccessfully](jsObject)(uploadedSuccessfullyFormat)
      case Some(value) => JsError(s"Unexpected value of _type: $value")
      case None => JsError("Missing _type field")
    }
  }

  implicit val writesUploadStatus: Writes[UploadStatus] = {
    case NotStarted => JsObject(Map("_type" -> JsString("NotStarted")))
    case InProgress => JsObject(Map("_type" -> JsString("InProgress")))
    case Failed => JsObject(Map("_type" -> JsString("Failed")))
    case FailedMimeType => JsObject(Map("_type" -> JsString("FailedMimeType")))
    case s: UploadedSuccessfully => Json.toJson(s)(uploadedSuccessfullyFormat).as[JsObject] + ("_type" -> JsString("UploadedSuccessfully"))
  }
}
