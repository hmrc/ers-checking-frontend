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
import play.api.i18n.{Messages, MessagesApi}

import java.io.File
import scala.io.Source
import scala.util.Using

class MessagesSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite {

  private val englishFile: File = app.environment.getFile("conf/messages.en")
  private val welshFile: File   = app.environment.getFile("conf/messages.cy")

  private lazy val englishMessages: Map[String, String] = parseMessages(englishFile)
  private lazy val welshMessages: Map[String, String]   = parseMessages(welshFile)

  private lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "The English and Welsh message files" should {

    "exist and not be empty" in {
      englishFile     should exist
      welshFile       should exist
      englishMessages should not be empty
      welshMessages   should not be empty
    }

    "be served by the application for both configured languages" in {
      messagesApi.messages.keySet                                    should contain allOf ("en", "cy")
      englishMessages.toSet.diff(messagesApi.messages("en").toSet) shouldBe empty
      welshMessages.toSet.diff(messagesApi.messages("cy").toSet)   shouldBe empty
    }

    "have no duplicate keys" in
      Seq(englishFile, welshFile).foreach { file =>
        withClue(s"${file.getName}: ") {
          duplicateKeys(file) shouldBe empty
        }
      }

    "have an English message for every Welsh key" in {
      val welshOnlyKeys = welshMessages.keySet.diff(englishMessages.keySet)
      withClue(describe(welshOnlyKeys, welshMessages)) {
        welshOnlyKeys shouldBe empty
      }
    }

    "have a Welsh message for every English key" in {
      val englishOnlyKeys = englishMessages.keySet.diff(welshMessages.keySet)
      withClue(describe(englishOnlyKeys, englishMessages)) {
        englishOnlyKeys shouldBe empty
      }
    }
  }

  private def parseMessages(file: File): Map[String, String] = {
    val url = file.toURI.toURL
    Messages.parse(Messages.UrlMessageSource(url), url.toString).fold(error => throw error, identity)
  }

  private def duplicateKeys(file: File): Set[String] = {
    val lines = Using.resource(Source.fromFile(file, "UTF-8"))(_.getLines().toList)
    val keys  = lines
      .foldLeft(List.empty[String]) {
        case (previous :: rest, line) if previous.endsWith("\\") => (previous.dropRight(1) + line) :: rest
        case (joined, line)                                      => line :: joined
      }
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .map(_.takeWhile(_ != '=').trim)
    keys.groupBy(identity).collect { case (duplicate, occurrences) if occurrences.size > 1 => duplicate }.toSet
  }

  private def describe(keys: Set[String], messages: Map[String, String]): String =
    keys.toSeq.sorted.map(key => s"$key: ${messages(key)}").mkString("\n", "\n", "\n")

}
