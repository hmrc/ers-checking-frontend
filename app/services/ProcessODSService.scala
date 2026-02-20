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

package services

import controllers.auth.RequestWithOptionalEmpRefAndPAYE
import models.upscan.UpscanCsvFilesCallback

import javax.inject.{Inject, Singleton}
import models.ERSFileProcessingException
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import repository.ErsCheckingFrontendSessionCacheRepository
import uk.gov.hmrc.validator.models.ValidationException
import utils.{ERSUtil, UploadedFileUtil}

import java.io.InputStream
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import models.SheetErrors.format
import uk.gov.hmrc.validator.models.ods.SheetErrors
import uk.gov.hmrc.validator.ods.OdsValidator
import uk.gov.hmrc.validator.validation.allTemplates

@Singleton
class ProcessODSService @Inject()(uploadedFileUtil: UploadedFileUtil,
                                  sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                  ersUtil: ERSUtil
                                 )(implicit ec: ExecutionContext) extends Logging {

  def performODSUpload(errorCount: Int, fileName: String, processor: InputStream, scheme: String)
                      (implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent], messages: Messages): Future[Try[Boolean]] = {
    try {
      checkFileType(fileName)
      val cacheFileName = sessionCacheService.cache[String](ersUtil.FILE_NAME_CACHE, fileName).recover {
        case e: Exception =>
          logger.error("[ProcessODSService][performODSUpload] Unable to save File Name. Error: " + e.getMessage)
          throw e
      }
      val sheetErrors: ListBuffer[SheetErrors] = validateOdsFile(fileName, processor, scheme)
      val cacheSheetErrors: Future[Try[Boolean]] = processSheetErrors(sheetErrors, None, errorCount)
      val result = for {
        _ <- cacheFileName
        v <- cacheSheetErrors
      } yield {
        v
      }
      result recover {
        case ex: Exception => Failure(ex)
      }
    }
    catch {
      case validationException: ValidationException =>
        logger.warn(s"[ProcessODSService][performODSUpload] ValidationException thrown trying to upload file - $validationException")
        Future.successful(Failure(validationException))
      case e: ERSFileProcessingException =>
        logger.warn(s"[ProcessODSService][performODSUpload] ERSFileProcessingException thrown trying to upload file - $e")
        Future.successful(Failure(e))
      case e: javax.xml.stream.XMLStreamException =>
        logger.warn(s"[ProcessODSService][performODSUpload] XMLStreamException - $e")
        Future.successful(Failure(e))
    }
  }
  def validateOdsFile(fileName: String, processor: InputStream, scheme: String): ListBuffer[SheetErrors] =
    OdsValidator.validateOdsFile(allTemplates, processor, scheme, fileName)

  // TODO: Can we remove the file argument?
  def processSheetErrors(sheetErrors: ListBuffer[SheetErrors], file: Option[UpscanCsvFilesCallback] = None, errorCount: Int)
                 (implicit request: Request[_]): Future[Try[Boolean]] = {
    if (isValid(sheetErrors)) {
      Future.successful(Success(true))
    }
    else {
      val updatedErrorCount: Int = sheetErrors.map(_.errors.length).sum
      val updatedErrorList = getSheetErrors(sheetErrors, errorCount)
      val id = if (file.isDefined) file.get.uploadId else ""

      val result = for {
        _ <- sessionCacheService.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}$id", updatedErrorCount)
        _ <- sessionCacheService.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}$id", updatedErrorList)
      } yield Success(false)

      result recover {
        case ex: Exception => Failure(ex)
      }
    }
  }

  def checkFileType(fileName: String)(implicit messages: Messages): Unit = {
    if (!uploadedFileUtil.checkODSFileType(fileName)) {
      throw ERSFileProcessingException(
        messages("ers_check_file.file_type_error", fileName),
        messages("ers_check_file.file_type_error", fileName))
    }
  }

  def isValid(schemeErrors: ListBuffer[SheetErrors]): Boolean = {
    schemeErrors.map(_.errors.isEmpty).forall(identity)
  }

  def getSheetErrors(schemeErrors: ListBuffer[SheetErrors], errorCount: Int): ListBuffer[SheetErrors] = {
    schemeErrors.map { schemeError =>
      SheetErrors(schemeError.sheetName, schemeError.errors.take(errorCount))
    }
  }
}
