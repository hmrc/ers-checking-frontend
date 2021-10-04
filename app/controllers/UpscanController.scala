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

package controllers

import akka.actor.ActorSystem
import config.ApplicationConfig
import controllers.auth.AuthAction

import javax.inject.Inject
import models.upscan._
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{ERSUtil, Retryable}

import scala.concurrent.{ExecutionContext, Future}

@Inject
class UpscanController @Inject()(authAction: AuthAction,
                                 sessionService: SessionService,
                                 mcc: MessagesControllerComponents,
                                 override val global_error: views.html.global_error,
                                 fileUploadProblemView: views.html.file_upload_problem
                                )(implicit executionContext: ExecutionContext, ersUtil: ERSUtil,
                                  actorSystem: ActorSystem, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with Retryable with I18nSupport with BaseController {

  val logger: Logger = Logger(getClass)


  def failure(): Action[AnyContent] = authAction.async { implicit request =>
    val errorCode = request.getQueryString("errorCode").getOrElse("No errorCode returned")
    val errorMessage = request.getQueryString("errorMessage").getOrElse("No errorMessage returned")
    val errorRequestId = request.getQueryString("errorRequestId").getOrElse("No errorRequestId returned")
    logger.error("[UpscanController][failure] Failed to upload file to Upscan")
    logger.error(s"Upscan Failure. errorCode: $errorCode, errorMessage: $errorMessage, errorRequestId: $errorRequestId")
    errorCode match {
      case "InvalidArgument" | "EntityTooLarge" | "EntityTooSmall" =>
        Future.successful(getFileUploadProblemPage)
      case _ => Future.successful(getGlobalErrorPage)
    }
  }

  def getFileUploadProblemPage()(implicit request: Request[AnyRef], messages: Messages): Result = {
    BadRequest(fileUploadProblemView(
      "ers.file_problem.title"
    )(request, messages, appConfig))
  }


  def fetchCsvCallbackList(list: UpscanCsvFilesList, sessionId: String)
                          (implicit hc: HeaderCarrier): Future[Seq[UpscanCsvFilesCallback]] = {
    Future.sequence {
      list.ids map { head =>
        ersUtil.fetch[UpscanIds](head.uploadId.value, sessionId) map { upscanId =>
          UpscanCsvFilesCallback(upscanId.uploadId, upscanId.uploadStatus)
        }
      }
    }
}

  def successCSV(uploadId: UploadId, scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    logger.info(s"[UpscanController][successCSV] Upload form submitted for ID: $uploadId")
    val sessionId = hc.sessionId.get.value

    val upscanCsvFilesList = for {
      csvFileList   <- ersUtil.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD, sessionId)
      updatedCacheFileList = {
        logger.info(s"[UpscanController][successCSV] Updating uploadId: ${uploadId.value} to InProgress")
        csvFileList.updateToInProgress(uploadId)
      }
      _ <- ersUtil.cache(ersUtil.CSV_FILES_UPLOAD, updatedCacheFileList, sessionId)
    } yield updatedCacheFileList

    upscanCsvFilesList flatMap { fileList =>
      if(fileList.noOfFilesToUpload == fileList.noOfUploads) {
        fetchCsvCallbackList(fileList, sessionId).withRetry(appConfig.allCsvFilesCacheRetryAmount){ list =>
          logger.debug(s"[UpscanController][successCSV] Fetched Callback list - $list")
          logger.info(s"[UpscanController][successCSV] Comparing cached files [${list.size}] to numberOfFileToUpload[${fileList.noOfFilesToUpload}]")
          logger.info(s"[UpscanController][successCSV] Checking if all files have completed upload - [${list.forall(_.isComplete)}]")


          (list.size == fileList.noOfFilesToUpload) && list.forall(_.isComplete)
        } map { files =>
          val callbackData = UpscanCsvFilesCallbackList(files.toList.reverse)
          sessionService.createCallbackRecordCSV(callbackData, sessionId)
          if(callbackData.areAllFilesSuccessful()) {
            Redirect(routes.UploadController.uploadCSVFile(scheme))
          } else {
            logger.error(s"[UpscanController][successCSV] Not all files are completed uploading - (${callbackData.areAllFilesComplete()}) " +
              s"or  had a successful response - (${callbackData.areAllFilesSuccessful()})")
            getGlobalErrorPage
          }
        } recover {
          case e: Exception =>
            logger.error(s"[UpscanController][successCSV] failed to fetch callback data with exception ${e.getMessage}," +
              s"timestamp: ${java.time.LocalTime.now()}.", e)
            getGlobalErrorPage
        }
      } else {
        Future.successful(Redirect(routes.CheckingServiceController.checkCSVFilePage()))
      }
    }

  }


  def successODS(scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    val futureCallbackData: Future[Option[UploadStatus]] = sessionService.getCallbackRecord.withRetry(appConfig.odsSuccessRetryAmount) {
      _.fold(true) {
          case _: UploadedSuccessfully | Failed => true
          case _ => false
        }
    }

    futureCallbackData flatMap {
      case Some(_: UploadedSuccessfully) =>
        Future.successful(Redirect(routes.UploadController.uploadODSFile(scheme)))
      case Some(Failed) =>
        logger.warn("[UpscanController][successODS] Upload status is failed")
        Future.successful(getGlobalErrorPage)
      case None =>
        logger.error(s"[UpscanController][successODS] Failed to verify upload. No data found in cache")
        Future.successful(getGlobalErrorPage)
    } recover {
      case e: LoopException[Option[UploadStatus]] =>
        logger.error(s"[UpscanController][successODS] Failed to verify upload. Upload status: ${e.finalFutureData.flatten}", e)
        getGlobalErrorPage
      case e: Exception =>
        logger.error(s"[UpscanController][successODS] Failed to save ods file with exception ${e.getMessage}.", e)
        getGlobalErrorPage
    }
  }
}
