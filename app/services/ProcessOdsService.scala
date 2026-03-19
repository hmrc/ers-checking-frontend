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
import models.ERSFileProcessingException
import models.SheetErrors.format
import play.api.Logging
import play.api.mvc.{AnyContent, Request}
import repository.ErsCheckingFrontendSessionCacheRepository
import uk.gov.hmrc.validator.SchemeVersion.All
import uk.gov.hmrc.validator.ValidatorException
import uk.gov.hmrc.validator.models.ods.SheetErrors
import uk.gov.hmrc.validator.ods.OdsValidator
import utils.ERSUtil

import java.io.InputStream
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ProcessOdsService @Inject()(sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                  ersUtil: ERSUtil
                                 )(implicit ec: ExecutionContext) extends Logging {

  def validateOdsFile(fileName: String, processor: InputStream, scheme: String): ListBuffer[SheetErrors] =
    OdsValidator.validateOdsFile(All, processor, scheme, fileName)

  def performOdsUpload(errorCount: Int, fileName: String, processor: InputStream, scheme: String)
                      (implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent]): Future[Boolean] = {
    for {
      _ <- sessionCacheService.cache[String](ersUtil.FILE_NAME_CACHE, fileName)
      sheetErrors: ListBuffer[SheetErrors] = validateOdsFile(fileName, processor, scheme)
      result: Boolean <- processSheetErrors(sheetErrors, errorCount)
    } yield result
  }.andThen {
    case Success(validFile: Boolean) => logger.info(s"[ProcessOdsService][performOdsUpload] Performed ods upload, file valid: $validFile")
    case Failure(e: NoSuchElementException) => logger.warn(s"[ProcessOdsService][performOdsUpload] Encountered NoSuchElementException - $e")
    case Failure(e: ValidatorException) => logger.warn(s"[ProcessOdsService][performOdsUpload] ValidationException thrown trying to upload file - $e")
    case Failure(e: ERSFileProcessingException) => logger.warn(s"[ProcessOsdService][performOdsUpload] ERSFileProcessingException thrown trying to upload file - $e")
    case Failure(e: javax.xml.stream.XMLStreamException) => logger.warn(s"[ProcessOdsService][performOdsUpload] XMLStreamException - $e")
  }

  def processSheetErrors(sheetErrors: ListBuffer[SheetErrors], errorCount: Int)
                        (implicit request: Request[_]): Future[Boolean] = {
    if (ProcessOdsService.isValid(sheetErrors)) {
      Future.successful(true)
    } else {
      val updatedErrorCount: Int = sheetErrors.map(_.errors.length).sum
      val updatedErrorList = ProcessOdsService.getSheetErrors(sheetErrors, errorCount)

      for {
        _ <- sessionCacheService.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}", updatedErrorCount)
        _ <- sessionCacheService.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}", updatedErrorList)
      } yield false
    }
  }
}

object ProcessOdsService {

  def isValid(schemeErrors: ListBuffer[SheetErrors]): Boolean = {
    schemeErrors.map(_.errors.isEmpty).forall(identity)
  }

  def getSheetErrors(schemeErrors: ListBuffer[SheetErrors], errorCount: Int): ListBuffer[SheetErrors] = {
    schemeErrors.map { schemeError =>
      SheetErrors(schemeError.sheetName, schemeError.errors.take(errorCount))
    }
  }

}