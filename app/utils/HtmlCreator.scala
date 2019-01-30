/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.i18n.Messages
import uk.gov.hmrc.services.validation.ValidationError
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.collection.mutable.ListBuffer

object HtmlCreator extends HtmlCreator
trait HtmlCreator {

	var uploadedFileUtil = UploadedFileUtil

	def getSheets(schemeErrors: ListBuffer[SheetErrors]): String = {
		var sheets: String = "";
		var errorListCount: Long = 0;
		val htmlTableColHeaders: String = Messages("ers_html_error_report.table_column_names")
		for (sheet <- schemeErrors) {
			val sheetErrors: ListBuffer[ValidationError] = sheet.errors
			if (sheet.errors.nonEmpty) {
				val htmlTableHead = Messages("ers_html_error_report.sheet_name", sheet.sheetName)
				var htmlTableRows: String = ""
				for (errors <- sheetErrors) {
						htmlTableRows = htmlTableRows + Messages("ers_html_error_report.table_row", errors.cell.column, errors.cell.row, errors.errorMsg)
						errorListCount = errorListCount + 1
				}
				if (errorListCount != 0) {
					sheets = sheets + Messages("ers_html_error_report.sheet", htmlTableHead, htmlTableColHeaders, htmlTableRows, sheet.sheetName)
				}
			}
		}
		sheets
	}

}
