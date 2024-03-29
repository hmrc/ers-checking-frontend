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

package models

import play.api.data.Forms._
import play.api.data._


object CSformMappings {

  /*
 * check file type Form definition
 */
  val checkFileTypeForm: Form[CS_checkFileType] = Form(
    mapping("checkFileType" -> optional(text).verifying("ers_check_file_type.alert", _.isDefined))
    (CS_checkFileType.apply)(CS_checkFileType.unapply)
  )

  /*
  * scheme type Form definition.
  */
  val schemeTypeForm: Form[CS_schemeType] = Form(
    mapping(
      "schemeType" -> optional(text).verifying("ers_scheme_type.select_scheme_type", _.isDefined)
    )(CS_schemeType.apply)(CS_schemeType.unapply)
  )

  def csvFileCheckForm(): Form[List[CsvFiles]] = Form(
    mapping(
      "files" -> list(
        mapping(
          "fileId" -> text.verifying("no_file_error", _.nonEmpty).verifying("invalidCharacters", id => fileIdList.contains(id))
        )
        (CsvFiles.apply)(CsvFiles.unapply)
      ))(List[CsvFiles])(Option[List[CsvFiles]]))

  val fileIdList = Seq(
    "EMI_ADJUSTMENTS", "EMI_REPLACED", "EMI_RCL", "EMI_NONTAXABLE", "EMI_TAXABLE",
    "CSOP_GRANTED", "CSOP_RCL", "CSOP_Exercised",
    "OTHER_GRANTS", "OTHER_OPTIONS", "OTHER_ACQUISITION", "OTHER_RESTRICTED", "OTHER_BENEFITS",
    "OTHER_CONVERTABLE", "OTHER_NOTIONAL", "OTHER_ENCHANCEMENT", "OTHER_SOLD",
    "SAYE_GRANTED", "SAYE_RCL", "SAYE_EXERCISED",
    "SIP_AWARDS", "SIP_OUT"
  )
}
