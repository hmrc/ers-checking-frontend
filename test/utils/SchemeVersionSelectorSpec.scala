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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.validator.SchemeVersion

class SchemeVersionSelectorSpec extends AnyWordSpecLike with Matchers {

  "getSchemeVersion" should {

    "return All when parsed true for useV4andV5Scheme and useV6andV7Scheme" in {
      SchemeVersionSelector.getSchemeVersion(
        useV4andV5Scheme = true,
        useV6andV7Scheme = true
      ) shouldBe SchemeVersion.All
    }

    "return V4andV5 when parsed true for useV4andV5Scheme and false for useV6andV7Scheme" in {
      SchemeVersionSelector.getSchemeVersion(
        useV4andV5Scheme = true,
        useV6andV7Scheme = false
      ) shouldBe SchemeVersion.V4andV5
    }

    "return V6andV7 when parsed false for useV4andV5Scheme and true for useV6andV7Scheme" in {
      SchemeVersionSelector.getSchemeVersion(
        useV4andV5Scheme = false,
        useV6andV7Scheme = true
      ) shouldBe SchemeVersion.V6andV7
    }

    "return V6andV7 when parsed false for useV4andV5Scheme and useV6andV7Scheme" in {
      SchemeVersionSelector.getSchemeVersion(
        useV4andV5Scheme = false,
        useV6andV7Scheme = false
      ) shouldBe SchemeVersion.V4andV5
    }
  }

}
