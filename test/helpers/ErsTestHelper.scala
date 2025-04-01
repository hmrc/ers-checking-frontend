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

package helpers

import config.ApplicationConfig
import controllers.auth.AuthAction
import metrics.Metrics
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, JsString, JsValue}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, stubBodyParser, stubControllerComponents, stubMessagesApi}
import play.twirl.api.Html
import repository.ErsCheckingFrontendSessionCacheRepository
import services.audit.AuditEvents
import services.validation.ErsValidator
import services.UpscanService
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.ERSUtil
import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

trait ErsTestHelper extends MockitoSugar { // scalastyle:off magic.number
  lazy val mockAuthAction = new AuthAction(mockAuthConnector, mockAppConfig, testBodyParser)
  lazy val authResultDefault: Enrolments = Enrolments(enrolments)
  val messagesActionBuilder: MessagesActionBuilder = new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
  val cc: ControllerComponents = stubControllerComponents()
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val testBodyParser: BodyParsers.Default = mock[BodyParsers.Default]
  val enrolments: Set[Enrolment] = Set(Enrolment("IR-PAYE", Seq(
    EnrolmentIdentifier("TaxOfficeNumber", "123"),
    EnrolmentIdentifier("TaxOfficeReference", "4567890")),
    "Activated"))
  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockErsValidator: ErsValidator = mock[ErsValidator]
  val realErsValidator: ErsValidator = new ErsValidator

  implicit val request: Request[AnyRef] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSessionId")))
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockMetrics: Metrics = mock[Metrics]
  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit val mockErsUtil: ERSUtil = mock[ERSUtil]
  val mockAuditEvents: AuditEvents = mock[AuditEvents]
  val mockSessionCacheRepo: ErsCheckingFrontendSessionCacheRepository = mock[ErsCheckingFrontendSessionCacheRepository]
  val mockUpscanService: UpscanService = mock[UpscanService]

  def doc(result: Html): Document = Jsoup.parse(contentAsString(result))

  def testMCC(app: Application): DefaultMessagesControllerComponents = {
    DefaultMessagesControllerComponents(
      messagesActionBuilder,
      DefaultActionBuilder(stubBodyParser[AnyContent]()),
      cc.parsers,
      app.injector.instanceOf[MessagesApi],
      cc.langs,
      cc.fileMimeTypes,
      ExecutionContext.global
    )
  }

  def mockAnyContentAction: OngoingStubbing[Future[Enrolments]] = {
    when(mockAuthConnector.authorise[Enrolments]
      (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(authResultDefault))
  }

  when(mockAppConfig.signIn).thenReturn("http://localhost:9553/bas-gateway/sign-in")
  when(mockAppConfig.signOut).thenReturn("http://localhost:9553/bas-gateway/sign-out-without-state")
  when(mockAppConfig.appName).thenReturn("ers-checking-frontend")
  when(mockAppConfig.loginCallback).thenReturn("http://localhost:9225/check-your-ers-files")
  when(mockAppConfig.mongoTTLInSeconds).thenReturn(3600)
  when(mockAppConfig.odsSuccessRetryAmount).thenReturn(5)
  when(mockAppConfig.odsValidationRetryAmount).thenReturn(1)

  when(mockAppConfig.retryDelay).thenReturn(3 milliseconds)
  when(mockAppConfig.errorCount).thenReturn(20)
  when(mockAppConfig.upscanFileSizeLimit).thenReturn(209715200)
  when(mockAppConfig.allCsvFilesCacheRetryAmount).thenReturn(3)

  //PageBuilder
  when(mockErsUtil.SCHEME_CSOP).thenReturn("1")
  when(mockErsUtil.SCHEME_EMI).thenReturn("2")
  when(mockErsUtil.SCHEME_OTHER).thenReturn("3")
  when(mockErsUtil.SCHEME_SAYE).thenReturn("4")
  when(mockErsUtil.SCHEME_SIP).thenReturn("5")
  when(mockErsUtil.OPTION_CSV).thenReturn("csv")
  when(mockErsUtil.OPTION_ODS).thenReturn("ods")
  when(mockErsUtil.OPTION_YES).thenReturn("1")
  when(mockErsUtil.OPTION_NO).thenReturn("2")
  when(mockErsUtil.OPTION_UPLOAD_SPREEDSHEET).thenReturn("1")
  when(mockErsUtil.OPTION_NIL_RETURN).thenReturn("2")

  //Cache Util
  when(mockErsUtil.CALLBACK_DATA_KEY).thenReturn("callback_data_key")
  when(mockErsUtil.CALLBACK_DATA_KEY_CSV).thenReturn("callback_data_key_csv")
  when(mockErsUtil.SCHEME_CACHE).thenReturn("scheme-type")
  when(mockErsUtil.FILE_TYPE_CACHE).thenReturn("check-file-type")
  when(mockErsUtil.SCHEME_ERROR_COUNT_CACHE).thenReturn("scheme-error-count")
  when(mockErsUtil.FILE_NAME_NO_EXTN_CACHE).thenReturn("file-name-no-extn")
  when(mockErsUtil.ERROR_LIST_CACHE).thenReturn("error-list")
  when(mockErsUtil.ERROR_SUMMARY_CACHE).thenReturn("error-summary")
  when(mockErsUtil.FORMAT_ERROR_CACHE).thenReturn("format_error")
  when(mockErsUtil.FORMAT_ERROR_CACHE_PARAMS).thenReturn("format_error_params")
  when(mockErsUtil.FORMAT_ERROR_EXTENDED_CACHE).thenReturn("format_extended_error")
  when(mockErsUtil.FILE_NAME_CACHE).thenReturn("file-name")
  when(mockErsUtil.CSV_FILES_UPLOAD).thenReturn("csv-files-upload")

  def generateTestCacheItem(id: String = "id",
                            data: Seq[(String, JsValue)] = Seq("" -> JsString(""))): CacheItem = {
    CacheItem(
      id = id,
      data = JsObject(data),
      createdAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC),
      modifiedAt = LocalDate.parse("2023-11-17").atStartOfDay().toInstant(ZoneOffset.UTC)
    )
  }
}
