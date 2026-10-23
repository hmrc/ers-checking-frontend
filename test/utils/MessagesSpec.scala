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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi

class MessagesSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite {

  val realMessages: Map[String, Map[String, String]] = app.injector.instanceOf[MessagesApi].messages
  val englishMessages: Map[String, String]           = realMessages("en")
  val welshMessages: Map[String, String]             = realMessages("cy")
  val defaultMessages: Map[String, String]           = realMessages("default")

  val uniqueEnglishMessages: Set[String] = englishMessages.keySet
  val uniqueWelshMessages: Set[String]   = welshMessages.keySet.diff(defaultMessages.keySet)

  "no duplicate messages in English set of messages" in {
    uniqueEnglishMessages.size shouldBe englishMessages.size
    uniqueEnglishMessages.size shouldBe uniqueWelshMessages.size
  }

  "all welsh messages should have a english translation" in {
    val missingWelshKeys = uniqueWelshMessages.filterNot(uniqueEnglishMessages)
    if (missingWelshKeys.nonEmpty) {
      val welshFailureText =
        missingWelshKeys.foldLeft(s"There are ${missingWelshKeys.size} missing Welsh translations:") {
          case (failureString, key) =>
            failureString + s"\n$key:${welshMessages(key)}"
        }
      fail(welshFailureText)
    }
  }

}
