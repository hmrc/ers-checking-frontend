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

package controllers.auth

import helpers.ErsTestHelper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.domain.EmpRef

class PAYEDetailsSpec extends Matchers with AnyWordSpecLike with ErsTestHelper {

  val payeDetailsWithDefaults: PAYEDetails =
    PAYEDetails(
      isAgent = true,
      agentHasPAYEEnrollement = true,
      None,
      mockAppConfig
    )

  Seq(
    // Agent redirects
    (payeDetailsWithDefaults, "/ers/agent/clients"),
    (payeDetailsWithDefaults.copy(agentHasPAYEEnrollement = false), "/not-enrolled-for-PAYE"),
    // Organisation redirects
    (payeDetailsWithDefaults.copy(isAgent = false, agentHasPAYEEnrollement = false), "/not-enrolled-for-PAYE"),
    (payeDetailsWithDefaults.copy(isAgent = false, agentHasPAYEEnrollement = false, Some(EmpRef("", ""))), "/not-enrolled-for-PAYE"),
    (payeDetailsWithDefaults.copy(isAgent = false, agentHasPAYEEnrollement = false, Some(EmpRef("1234", "test"))), "/ers/org/1234/test/schemes")
  ).foreach {
    case (details, expectedRedirect) =>
      val empRefValueString = details.optionalEmpRef.map(empRef => s" (value: ${empRef.value})").getOrElse("")
      s"getPAYERedirect should return ${expectedRedirect} when passed " +
        s"isAgent: ${details.isAgent}, " +
        s"agentHasPAYEEnrollement: ${details.agentHasPAYEEnrollement} and " +
        s"optionalEmpRef.isDefined: ${details.optionalEmpRef.isDefined}${empRefValueString}" in {
        details.getPAYERedirectCall.url.contains(expectedRedirect)
      }
  }

  Seq(
    // Agent redirects
    (payeDetailsWithDefaults, "/ers/agent/clients"),
    // Organisation redirects
    (payeDetailsWithDefaults.copy(isAgent = false), "/business-account")
  ).foreach {
    case (details, expectedRedirect) =>
      s"getSignOutRedirect should return ${expectedRedirect} when passed isAgent: ${details.isAgent}" in {
        details.getSignOutRedirectCall.url shouldBe(expectedRedirect)
      }
  }


}
