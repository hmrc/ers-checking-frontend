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

package views

import helpers.ErsTestHelper
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, Messages, MessagesImpl}
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import views.html.format_errors

class FormatErrorsViewSpec extends ViewSpecBase with ErsTestHelper {

  class FormatErrorsViewSetUp(language: String) {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
    implicit val messages: Messages                           = MessagesImpl(Lang(language), messagesApi)

    val view: format_errors = app.injector.instanceOf[format_errors]

    def generateFailureDoc(errorMsg: String, errorMsgParams: List[String]): Document = asDocument(
      view(
        fileType = "csv",
        schemeName = "ers_pdf_error_report.csop",
        schemeNameShort = "CSOP",
        errorMsg = errorMsg,
        errorMsgParams = errorMsgParams,
        showExtendedInstructions = false,
        uploadAgainLink = Call("GET", "/check-your-ers-files/choose-and-check-csv-files?")
      )
    )

    val incorrectSchemeTypeDoc: Document =
      generateFailureDoc(
        errorMsg = "ers.exceptions.dataParser.incorrectSchemeType",
        errorMsgParams = List("an EMI", "a CSOP", "EMI40_RLC_V4")
      )

    val noDataFailureDoc: Document =
      generateFailureDoc(
        errorMsg = "ers.exceptions.dataParser.noData",
        errorMsgParams = List.empty[String]
      )

    val dataContainsAmpersandFailureDoc: Document =
      generateFailureDoc(
        errorMsg = "ers.exceptions.dataParser.ampersand",
        errorMsgParams = List.empty[String]
      )

    val incorrectHeaderFailureDoc: Document =
      generateFailureDoc(
        errorMsg = "ers.exceptions.dataParser.incorrectHeader",
        errorMsgParams = List("EMI_V4", "EMI_file")
      )

    val incorrectSheetNameFailureDoc: Document =
      generateFailureDoc(
        errorMsg = "ers.exceptions.dataParser.incorrectSheetName",
        errorMsgParams = List("EMI_V4", "CSOP_V4")
      )

    val sheetNameNotInSchemeVersionFailureDoc: Document =
      generateFailureDoc(
        errorMsg = "ers.exceptions.dataParser.wrongSchemeFile",
        errorMsgParams = List.empty[String]
      )

    def getErrorMessage(doc: Document): String = doc.getElementsByClass("govuk-inset-text").text()
  }

  "format errors view" should {

    "display the correct error message for incorrect scheme type exception" when {
      "using the english message keys" in new FormatErrorsViewSetUp("en") {
        val expectedErrorMessage = "You chose to check a CSOP file, but EMI40_RLC_V4 is an EMI file."
        getErrorMessage(incorrectSchemeTypeDoc) mustBe expectedErrorMessage
      }

      "when using the welsh message keys" in new FormatErrorsViewSetUp("cy") {
        val expectedErrorMessage = "Gwnaethoch ddewis gwirio ffeil a CSOP, ond ffeil an EMI yw EMI40_RLC_V4."
        getErrorMessage(incorrectSchemeTypeDoc) mustBe expectedErrorMessage
      }
    }

    "display the correct error message for no data exception" when {
      "when using the english message keys" in new FormatErrorsViewSetUp("en") {
        val expectedErrorMessage =
          "The file that you chose doesn’t have any data after row 9. The reportable events data must start in cell A10. " +
            "Use the Employment Related Securities (ERS) guidance documents to help you create error-free files."
        getErrorMessage(noDataFailureDoc) mustBe expectedErrorMessage
      }

      "when using the welsh message keys" in new FormatErrorsViewSetUp("cy") {
        val expectedErrorMessage = "Nid oes unrhyw ddata ar ôl rhes 9 yn y ffeil a ddewisoch. Mae’n rhaid i ddata’r " +
          "digwyddiadau hysbysadwy ddechrau yng nghell A10. Defnyddiwch y dogfennau arweiniad Gwarantau Ar Sail " +
          "Cyflogaeth (ERS) i’ch helpu i greu ffeiliau heb wallau."
        getErrorMessage(noDataFailureDoc) mustBe expectedErrorMessage
      }
    }

    "display the correct error message for data contains ampersand exception" when {
      "when using the english message keys" in new FormatErrorsViewSetUp("en") {
        val expectedErrorMessage = "Must not contain ampersands."
        getErrorMessage(dataContainsAmpersandFailureDoc) mustBe expectedErrorMessage
      }

      "when using the welsh message keys" in new FormatErrorsViewSetUp("cy") {
        val expectedErrorMessage = "Rhaid peidio â chynnwys ampersandau."
        getErrorMessage(dataContainsAmpersandFailureDoc) mustBe expectedErrorMessage
      }
    }

    "display the correct error message for incorrect header exception" when {
      "when using the english message keys" in new FormatErrorsViewSetUp("en") {
        val expectedErrorMessage = "The headers on EMI_V4 in EMI_file aren’t right. Use the Employment Related " +
          "Securities (ERS) guidance documents to make sure they’re as required."
        getErrorMessage(incorrectHeaderFailureDoc) mustBe expectedErrorMessage
      }

      "when using the welsh message keys" in new FormatErrorsViewSetUp("cy") {
        val expectedErrorMessage = "Nid yw’r penynnau ar EMI_V4 yn EMI_file yn gywir. Defnyddiwch y dogfennau " +
          "arweiniad Gwarantau Ar Sail Cyflogaeth (ERS) i sicrhau eu bod yn bodloni gofynion."
        getErrorMessage(incorrectHeaderFailureDoc) mustBe expectedErrorMessage
      }
    }

    "display the correct error message for incorrect sheet name exception" when {
      "when using the english message keys" in new FormatErrorsViewSetUp("en") {
        val expectedErrorMessage = "EMI_V4 isn’t a sheet name (title) that can be used in CSOP_V4 files. Use the " +
          "Employment Related Securities (ERS) guidance documents to help you create error-free files."
        getErrorMessage(incorrectSheetNameFailureDoc) mustBe expectedErrorMessage
      }

      "when using the welsh message keys" in new FormatErrorsViewSetUp("cy") {
        val expectedErrorMessage = "Nid yw EMI_V4 yn (deitl) enw dalen all gael ei ddefnyddio yn ffeiliau CSOP_V4. " +
          "Defnyddiwch y dogfennau arweiniad Gwarantau Ar Sail Cyflogaeth (ERS) i’ch helpu i greu ffeiliau heb wallau."
        getErrorMessage(incorrectSheetNameFailureDoc) mustBe expectedErrorMessage
      }
    }

    "display the correct error message for sheet name not in scheme version exception" when {
      "when using the english message keys" in new FormatErrorsViewSetUp("en") {
        val expectedErrorMessage = "This is not a file that you said you needed to upload, choose a different file"
        getErrorMessage(sheetNameNotInSchemeVersionFailureDoc) mustBe expectedErrorMessage
      }

      "when using the welsh message keys" in new FormatErrorsViewSetUp("cy") {
        val expectedErrorMessage =
          "Nid yw hon yn ffeil y rhoesoch wybod fod angen i chi ei huwchlwytho – dewiswch ffeil wahanol"
        getErrorMessage(sheetNameNotInSchemeVersionFailureDoc) mustBe expectedErrorMessage
      }
    }
  }

}
