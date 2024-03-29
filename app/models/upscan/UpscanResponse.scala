/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.Call

case class UpscanInitiateResponse(
                                   fileReference: Reference,
                                   postTarget: Call,
                                   formFields: Map[String, String]
                                 )

case class Reference(value: String) extends AnyVal

case class UploadForm(href: String, fields: Map[String, String])
object Reference {
  implicit val referenceReader: Reads[Reference] = Reads.StringReads.map(Reference(_))
  implicit val referenceWrites: Writes[Reference] = Writes[Reference](x => JsString(x.value))
}

case class PreparedUpload(reference: Reference, uploadRequest: UploadForm) {
  def toUpscanInitiateResponse: UpscanInitiateResponse = {
    val fileReference = reference
    val postTarget    = Call("post", uploadRequest.href)
    val formFields    = uploadRequest.fields
    UpscanInitiateResponse(fileReference, postTarget, formFields)
  }
}
object PreparedUpload {
  implicit val uploadFormFormat: Format[UploadForm] = Json.format[UploadForm]
  implicit val format: Format[PreparedUpload] = Json.format[PreparedUpload]
}
