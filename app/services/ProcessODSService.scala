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

import javax.inject.{Inject, Singleton}
import models.ERSFileProcessingException
import play.api.Logging
import play.api.mvc.{AnyContent, Request}
import repository.ErsCheckingFrontendSessionCacheRepository
import utils.ERSUtil

import java.io.InputStream
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import models.SheetErrors.format
import uk.gov.hmrc.validator.SchemeVersion.All
import uk.gov.hmrc.validator.ValidatorException
import uk.gov.hmrc.validator.models.ods.SheetErrors
import uk.gov.hmrc.validator.ods.OdsValidator

@Singleton
class ProcessODSService @Inject()(sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                  ersUtil: ERSUtil
                                 )(implicit ec: ExecutionContext) extends Logging {

  def validateOdsFile(fileName: String, processor: InputStream, scheme: String): ListBuffer[SheetErrors] =
    OdsValidator.validateOdsFile(All, processor, scheme, fileName)

  def performODSUpload(errorCount: Int, fileName: String, processor: InputStream, scheme: String)
                      (implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent]): Future[Either[Exception, Boolean]] = {
    for {
      _ <- sessionCacheService.cache[String](ersUtil.FILE_NAME_CACHE, fileName)
      sheetErrors: ListBuffer[SheetErrors] = validateOdsFile(fileName, processor, scheme)
      result: Either[Exception, Boolean] <- processSheetErrors(sheetErrors, errorCount)
    } yield result
  }.recover{
    case noSuchElementException: NoSuchElementException =>
      logger.warn(s"[ProcessODSService][performODSUpload] Encountered NoSuchElementException - $noSuchElementException")
      Left(noSuchElementException)
    case validatorException: ValidatorException =>
      logger.warn(s"[ProcessODSService][performODSUpload] ValidationException thrown trying to upload file - $validatorException")
      Left(validatorException)
    case e: ERSFileProcessingException =>
      logger.warn(s"[ProcessODSService][performODSUpload] ERSFileProcessingException thrown trying to upload file - $e")
      Left(e)
    case e: javax.xml.stream.XMLStreamException =>
      logger.warn(s"[ProcessODSService][performODSUpload] XMLStreamException - $e")
      Left(e)
  }

  def processSheetErrors(sheetErrors: ListBuffer[SheetErrors], errorCount: Int)
                 (implicit request: Request[_]): Future[Either[Exception, Boolean]] = {
    if (ProcessODSService.isValid(sheetErrors)) {
      Future.successful(Right(true))
    }
    else {
      val updatedErrorCount: Int = sheetErrors.map(_.errors.length).sum
      val updatedErrorList = ProcessODSService.getSheetErrors(sheetErrors, errorCount)

      val result = for {
        _ <- sessionCacheService.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}", updatedErrorCount)
        _ <- sessionCacheService.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}", updatedErrorList)
      } yield Right(false)

      result recover {
        case ex: Exception => Left(ex)
      }
    }
  }

}

object ProcessODSService {

  def isValid(schemeErrors: ListBuffer[SheetErrors]): Boolean = {
    schemeErrors.map(_.errors.isEmpty).forall(identity)
  }

  def getSheetErrors(schemeErrors: ListBuffer[SheetErrors], errorCount: Int): ListBuffer[SheetErrors] = {
    schemeErrors.map { schemeError =>
      SheetErrors(schemeError.sheetName, schemeError.errors.take(errorCount))
    }
  }

}
