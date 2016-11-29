/*
 * Copyright 2016 HM Revenue & Customs
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

import java.io.File

object UploadedFileUtil extends UploadedFileUtil

trait UploadedFileUtil {

	def checkODSFileType(fileName: String): Boolean = {
		val delimiter: Char = '.'
		val stringTokens: Array[String] = fileName.split(delimiter)
		stringTokens(stringTokens.length - 1) match {
			case "ods" => true
			case _ => false
		}
	}

	def checkCSVFileType(fileName: String): Boolean = {
		println(s"\n\n ********* \n checkCSVFileType: filename = ${fileName}\n ************ \n ")

		val delimiter: Char = '.'
		val stringTokens: Array[String] = fileName.split(delimiter)
		val y = stringTokens(stringTokens.length - 1) match {
			case "csv" => true
			case _ => false
		}
		println(s"\n\n ********* \n checkCSVFileType: result =  ${y}\n ************ \n ")

		y
	}

	def deleteFile(file: File): Unit = {

		file.getCanonicalFile.delete()
		file.getAbsoluteFile.delete()

	}

}
