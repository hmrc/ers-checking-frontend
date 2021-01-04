/*
 * Copyright 2021 HM Revenue & Customs
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

import config.{ApplicationConfig, ERSShortLivedCache}
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
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, stubBodyParser, stubControllerComponents, stubMessagesApi}
import play.twirl.api.Html
import services.audit.AuditEvents
import services.{SessionService, UpscanService}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}

trait ErsTestHelper extends MockitoSugar {

	def doc(result: Html): Document = Jsoup.parse(contentAsString(result))

	val messagesActionBuilder: MessagesActionBuilder = new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
	val cc: ControllerComponents = stubControllerComponents()

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

	val mockAuthConnector: AuthConnector = mock[AuthConnector]
	val testBodyParser: BodyParsers.Default = mock[BodyParsers.Default]
	lazy val mockAuthAction = new AuthAction(mockAuthConnector, mockAppConfig, testBodyParser)
	val enrolments = Set(Enrolment("IR-PAYE", Seq(
		EnrolmentIdentifier("TaxOfficeNumber", "123"),
		EnrolmentIdentifier("TaxOfficeReference", "4567890")),
		"Activated"))

	lazy val authResultDefault: Enrolments = Enrolments(enrolments)

	def mockAnyContentAction: OngoingStubbing[Future[Enrolments]] = {
		when(mockAuthConnector.authorise[Enrolments]
			(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
			.thenReturn(Future.successful(authResultDefault))
	}

	implicit val request: Request[_] = FakeRequest()
	implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSessionId")))
	implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

	val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
	val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
	val mockErsUtil: ERSUtil = mock[ERSUtil]
	val mockMetrics: Metrics = mock[Metrics]
	val mockAuditEvents: AuditEvents = mock[AuditEvents]
	val mockShortLivedCache: ERSShortLivedCache = mock[ERSShortLivedCache]
	val mockSessionService: SessionService = mock[SessionService]
	val mockUpscanService: UpscanService = mock[UpscanService]

	when(mockAppConfig.signIn).thenReturn("http://localhost:9553/bas-gateway/sign-in")
	when(mockAppConfig.appName).thenReturn("ers-checking-frontend")
	when(mockAppConfig.loginCallback).thenReturn("http://localhost:9225/check-your-ers-files")
	when(mockAppConfig.odsSuccessRetryAmount).thenReturn(5)
	when(mockAppConfig.odsValidationRetryAmount).thenReturn(1)

	import scala.concurrent.duration._
	when(mockAppConfig.retryDelay).thenReturn(3 milliseconds)
	when(mockAppConfig.errorCount).thenReturn(Option(20))
	when(mockAppConfig.chunkSize).thenReturn(Option(25000))
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
}
