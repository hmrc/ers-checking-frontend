/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Play}

trait WithFakeApplication extends BeforeAndAfterAll {
  this: Suite =>

  implicit val app: Application = new GuiceApplicationBuilder().build()

  override def beforeAll() {
    super.beforeAll()
    Play.start(app)
  }

  override def afterAll() {
    super.afterAll()
  }
}
