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

import config.ApplicationConfig
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.start

class StartViewSpec extends ViewSpecBase {

  class StartViewSetUp(bannerEnabled: Boolean, bannerSecondParagraphEnabled: Boolean) {

    implicit val applicationConfig = new ApplicationConfig(mock[ServicesConfig]) {
      override lazy val startPageBannerEnabled = bannerEnabled
      override lazy val startPageBannerParaTwoEnabled = bannerSecondParagraphEnabled
    }

    implicit val request: FakeRequest[AnyRef] = fakeRequest
    implicit val messages: Messages = testMessages

    val view: start = app.injector.instanceOf[start]

    def getBanner(doc: Document): String = doc.getElementsByClass("govuk-notification-banner").text()

    val bannerText: String = "Important From 6 April 2027 you must use the updated version of the HMRC templates when " +
      "you submit your ERS return. If you create your own files you will need to use the technical notes to update your file."
    val secondBannerParagraph: String = " You can use the checking service to check your updated files from 1 " +
      "February 2027."
  }

  "start view" should {

    "show not show the banner when it is disabled" in new StartViewSetUp(false, false) {
      val doc: Document = asDocument(view(request, messages, applicationConfig))
      getBanner(doc) mustBe ""
    }

    "show the banner with only the first paragraph when the banner is enabled but the second paragraph is disabled" in new StartViewSetUp(true, false) {
      val doc: Document = asDocument(view(request, messages, applicationConfig))
      getBanner(doc) mustBe bannerText
    }

    "show the banner with second paragraph when both are enabled" in new StartViewSetUp(true, true) {
      val doc: Document = asDocument(view(request, messages, applicationConfig))
      getBanner(doc) mustBe(bannerText + secondBannerParagraph)
    }

  }
}
