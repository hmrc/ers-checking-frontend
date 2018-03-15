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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

class ContentUtilSpec  extends UnitSpec with MockitoSugar with OneAppPerSuite{

  val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

  implicit val hc = HeaderCarrier()

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()

  "ContentUtil" should {
    val data = List(
      ("1","csop", "Company Share Option Plan"),
      ("2","emi", "Enterprise Management Incentives"),
      ("3","other", "Other"),
      ("4","saye", "Save As You Earn"),
      ("5","sip", "Share Incentive Plan")
    )
    for(schemeType <- data) {
      s"return scheme name and abbreviation for ${schemeType._2}" in {
        ContentUtil.getSchemeName(schemeType._1)._2 shouldBe schemeType._2.toUpperCase
      }
    }
  }
}
