/*
 * Copyright 2026 HM Revenue & Customs
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

package services

object XMLTestHelpers {

  def buildValidOdsXml(body: String) =
    s"""<office:document-content
      xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
      xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
      xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      xmlns:calcext="urn:org:documentfoundation:names:experimental:calc:xmlns:calcext:1.0"
      office:version="1.3">
    <office:body>
      <office:spreadsheet>
        $body
      </office:spreadsheet>
    </office:body>
  </office:document-content>"""

  def openTable(sheetName: String, styleName: String = "ta1"): String =
    s"""<table:table table:style-name="$styleName" table:name="$sheetName">""".stripMargin

  val closeTable = """</table:table>"""

}
