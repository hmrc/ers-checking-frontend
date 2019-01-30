/*
 * Copyright 2019 HM Revenue & Customs
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

package services

import akka.actor.ActorSystem
import config.WSHttp
import play.api.Play
import uk.gov.hmrc.play.config.{AppName, RunMode}

object AllWsHttp extends WSAllMethods with WSHttp with AppName with RunMode {
  override protected def mode: play.api.Mode.Mode = Play.current.mode
  override protected def runModeConfiguration: play.api.Configuration = Play.current.configuration
  override protected def appNameConfiguration: play.api.Configuration = runModeConfiguration
  override protected def actorSystem : ActorSystem = Play.current.actorSystem
  override protected val configuration : scala.Option[com.typesafe.config.Config] = Option(runModeConfiguration.underlying)

  override val hooks = Seq.empty
}
