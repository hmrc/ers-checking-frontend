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

import config.ApplicationConfig
import controllers.routes
import play.api.mvc.Call
import uk.gov.hmrc.domain.EmpRef

case class PAYEDetails(
    isAgent: Boolean,
    agentHasPAYEEnrollement: Boolean,
    optionalEmpRef: Option[EmpRef],
    appConfig: ApplicationConfig
                      ){

  val dassAgentClientRedirect: Call = Call.apply("GET", appConfig.dassAgentClientsPath)
  val addBusinessTaxAccountRedirect: Call = Call.apply("GET", appConfig.addBusinessTaxAccountPath)

  def getAgentPAYERedirect(agentHasPAYEEnrollement: Boolean): Call =
    if (agentHasPAYEEnrollement){
      dassAgentClientRedirect
    }
    else {
      routes.CheckPAYEController.missingPAYE()
    }

  def getOrgPAYERedirect(optionalEmpRef: Option[EmpRef]): Call = {
    optionalEmpRef
      .filter(_.value != "/") match {
        case Some(empRef: EmpRef) => {
          val orgPAYRRedirectUrl = s"/ers/org/${empRef.value}/schemes"
          Call.apply("GET", orgPAYRRedirectUrl)
        }
        case None =>
          routes.CheckPAYEController.missingPAYE()
      }
  }

  val getPAYERedirect: Call =
    if (isAgent) {
      getAgentPAYERedirect(agentHasPAYEEnrollement)
    }
    else {
      getOrgPAYERedirect(optionalEmpRef)
    }

  val getSignOutRedirect: Call =
    if (isAgent) {
      dassAgentClientRedirect
    }
    else {
      addBusinessTaxAccountRedirect
    }
}
