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

package metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import play.api.Logger
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

trait ERSMetrics {
  def dataIteratorTimer(diff: Long, unit: TimeUnit): Unit
}

object ERSMetrics extends ERSMetrics with MicroserviceMetrics {

  val logger = Logger(this.getClass.getCanonicalName)
  lazy val registry: MetricRegistry = metrics.defaultRegistry

  override def dataIteratorTimer(diff: Long, unit: TimeUnit): Unit =
    try {
      registry.timer("data-iterator-time").update(diff, unit)
    } catch {
      case t: Throwable => logger.warn("Unable to initialise MetricRegistry, timer will not be created.", t)
    }
}

trait Metrics {
  val metrics:ERSMetrics = ERSMetrics
}
