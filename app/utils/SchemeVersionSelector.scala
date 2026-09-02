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

package utils

import play.api.Logging
import uk.gov.hmrc.validator.SchemeVersion

object SchemeVersionSelector extends Logging {

  def getSchemeVersion(useV4andV5Scheme: Boolean, useV6andV7Scheme: Boolean): SchemeVersion =
    (useV4andV5Scheme, useV6andV7Scheme) match {
      case (true, true)   => SchemeVersion.All
      case (true, false)  => SchemeVersion.V4andV5
      case (false, true)  => SchemeVersion.V6andV7
      case (false, false) =>
        logger.info(
          "[SchemeVersionSelector][getSchemeVersion] PARSED FALSE FALSE for getSchemeVersion, using V4andV5" +
            " as a fall back"
        )
        SchemeVersion.V4andV5
    }

}
