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

package controllers

import org.apache.pekko.actor.ActorSystem
import config.ApplicationConfig
import controllers.auth.AuthAction
import models.upscan._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import repository.ErsCheckingFrontendSessionCacheRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{ERSUtil, Retryable}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Inject
class UpscanController @Inject()(authAction: AuthAction,
                                 sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                 mcc: MessagesControllerComponents,
                                 override val global_error: views.html.global_error
                                )(implicit executionContext: ExecutionContext, ersUtil: ERSUtil,
                                  actorSystem: ActorSystem, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with Retryable with I18nSupport with ErsBaseController {

  val logger: Logger = Logger(getClass)

  def failure(): Action[AnyContent] = authAction.async { implicit request =>
    val errorCode = request.getQueryString("errorCode").getOrElse("No errorCode returned")
    val errorMessage = request.getQueryString("errorMessage").getOrElse("No errorMessage returned")
    val errorRequestId = request.getQueryString("errorRequestId").getOrElse("No errorRequestId returned")
    logger.error("[UpscanController][failure] Failed to upload file to Upscan")
    logger.error(s"Upscan Failure. errorCode: $errorCode, errorMessage: $errorMessage, errorRequestId: $errorRequestId")
    errorCode match {
      case "EntityTooLarge" | "EntityTooSmall" =>
        Future.successful(Redirect(routes.CheckingServiceController.checkingInvalidFilePage()))
      case _ => Future.successful(getGlobalErrorPage)
    }
  }

  def fetchCsvCallbackList(list: UpscanCsvFilesList)
                          (implicit request: Request[_]): Future[Seq[UpscanCsvFilesCallback]] = {
    Future.sequence {
      list.ids map { head =>
        sessionCacheService.fetchAndGetEntry[UpscanIds](head.uploadId.value) map { upscanId =>
          UpscanCsvFilesCallback(upscanId.uploadId, upscanId.uploadStatus)
        }
      }
    }
  }

  def successCsv(uploadId: UploadId, scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    logger.info(s"[UpscanController][successCsv] Upload form submitted for ID: $uploadId")

    val upscanCsvFilesList = for {
      csvFileList   <- sessionCacheService.fetchAndGetEntry[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD)
      updatedCacheFileList = {
        logger.info(s"[UpscanController][successCsv] Updating uploadId: ${uploadId.value} to InProgress")
        csvFileList.updateToInProgress(uploadId)
      }
      _ <- sessionCacheService.cache(ersUtil.CSV_FILES_UPLOAD, updatedCacheFileList)
    } yield updatedCacheFileList

    upscanCsvFilesList flatMap { fileList =>
      if(fileList.noOfFilesToUpload == fileList.noOfUploads) {
        fetchCsvCallbackList(fileList).withRetry(appConfig.allCsvFilesCacheRetryAmount){ list =>
          logger.debug(s"[UpscanController][successCsv] Fetched Callback list - $list")
          logger.info(s"[UpscanController][successCsv] Comparing cached files [${list.size}] to numberOfFileToUpload[${fileList.noOfFilesToUpload}]")
          logger.info(s"[UpscanController][successCsv] Checking if all files have completed upload - [${list.forall(_.isComplete)}]")

          (list.size == fileList.noOfFilesToUpload) && list.forall(_.isComplete)
        } flatMap  { files =>
          val callbackData = UpscanCsvFilesCallbackList(files.toList.reverse)
          logger.debug(s"[UpscanController][successCsv] - before adding into mongo: $callbackData")
          sessionCacheService.cache[UpscanCsvFilesCallbackList](ersUtil.CALLBACK_DATA_KEY_CSV, callbackData).map{ ele =>
            if(callbackData.areAllFilesSuccessful()) {
              logger.debug(s"[UpscanController][successCsv] - callback upscan successful, callback: $callbackData")
              Redirect(routes.UploadController.uploadCsvFile(scheme))
            } else if (callbackData.areAnyFilesWrongMimeType()) {
              logger.info(s"[UpscanController][successCsv] - callback upscan rejected due to wrong mime type")
              Redirect(routes.CheckingServiceController.checkingInvalidFilePage())
            } else {
              logger.error(s"[UpscanController][successCsv] Not all files are completed uploading - (${callbackData.areAllFilesComplete()}) " +
                s"or  had a successful response - (${callbackData.areAllFilesSuccessful()})")
              getGlobalErrorPage
            }
          }
        } recover {
          case e: Exception =>
            logger.error(s"[UpscanController][successCsv] failed to fetch callback data with exception ${e.getMessage}," +
              s"timestamp: ${java.time.LocalTime.now()}.", e)
            getGlobalErrorPage
        }
      } else {
        logger.info(s"[UpscanController][successCsv] - checking the number of files upload to checking service, fileList: $fileList")
        Future.successful(Redirect(routes.CheckingServiceController.checkCsvFilePage()))
      }
    } recover {
      case e: Exception =>
        logger.error(s"[UpscanController][successCsv] Failed to update the ids with exception ${e.getMessage}.", e)
        getGlobalErrorPage
    }

  }

  def successOds(scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    val futureCallbackData: Future[Option[UploadStatus]] = sessionCacheService.fetch[UploadStatus](
      ersUtil.CALLBACK_DATA_KEY).withRetry(appConfig.odsSuccessRetryAmount) {
      _.fold(true) {
          case _: UploadedSuccessfully | Failed | FailedMimeType => true
          case _ => false
        }
    }

    futureCallbackData flatMap {
      case Some(_: UploadedSuccessfully) =>
        Future.successful(Redirect(routes.UploadController.uploadOdsFile(scheme)))
      case Some(Failed) =>
        logger.warn("[UpscanController][successOds] Upload status is failed")
        Future.successful(getGlobalErrorPage)
      case Some(FailedMimeType) =>
        logger.warn("[UpscanController][successOds] Upload status is rejected")
        Future.successful(Redirect(routes.CheckingServiceController.checkingInvalidFilePage()))
      case Some(status) =>
        logger.warn(s"[UpscanController][successOds] Invalid upload status: $status")
        Future.successful(getGlobalErrorPage)
      case None =>
        logger.error(s"[UpscanController][successOds] Failed to verify upload. No data found in cache")
        Future.successful(getGlobalErrorPage)
    } recover {
      case e: LoopException[Option[UploadStatus] @unchecked] =>
        logger.error(s"[UpscanController][successOds] Failed to verify upload. Upload status: ${e.finalFutureData.flatten}", e)
        getGlobalErrorPage
      case e: Exception =>
        logger.error(s"[UpscanController][successOds] Failed to save ods file with exception ${e.getMessage}.", e)
        getGlobalErrorPage
    }
  }
}
