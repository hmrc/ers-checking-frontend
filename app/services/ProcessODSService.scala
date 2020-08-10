/*
 * Copyright 2020 HM Revenue & Customs
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

import java.io.File

import controllers.auth.RequestWithOptionalEmpRef
import models.{ERSFileProcessingException, FileObject, SheetErrors}
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.Files
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{CacheUtil, ParserUtil, UploadedFileUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Try}

object ProcessODSService extends ProcessODSService {
  override val uploadedFileUtil: UploadedFileUtil = UploadedFileUtil
  override val cacheUtil:CacheUtil = CacheUtil
}

trait ProcessODSService {
  val uploadedFileUtil: UploadedFileUtil
  val cacheUtil:CacheUtil

  def performODSUpload(fileName: String, processor: StaxProcessor)
											(implicit request: RequestWithOptionalEmpRef[AnyContent], scheme:String, hc : HeaderCarrier, messages: Messages): Future[Try[Boolean]] = {
		try {
			val errorList: ListBuffer[SheetErrors] = checkFileType(processor, fileName)(scheme, hc, request, messages)
			val cache = cacheUtil.cache[String](CacheUtil.FILE_NAME_CACHE, fileName).recover {
				case e: Exception =>
					Logger.error("[ProcessODSService][performODSUpload] Unable to save File Name. Error: " + e.getMessage)
					throw e
			}
			val valid = ParserUtil.isFileValid(errorList, "performODSUpload")
			val result = for {
				_ <- cache
				v <- valid
			} yield {
				v
			}
			result recover {
				case ex: Exception => Failure(ex)
			}
		}
		catch {
			case e: ERSFileProcessingException =>
				Logger.warn(s"[ProcessODSService][performODSUpload] ERSFileProcessingException thrown trying to upload file - $e")
				Future.successful(Failure(e))
		}
	}

  def createFileObject(uploadedFile: Seq[MultipartFormData.FilePart[Files.TemporaryFile]])
                      (implicit request: Request[AnyContent]): (Boolean, java.util.ArrayList[FileObject]) = {
    val fileSet = uploadedFile.map(file => file.key)
    val fileSetLength = fileSet.length
    val fileObjectList = new java.util.ArrayList[FileObject](fileSetLength)
    var fileParam:String = ""
    var validFileExtn: Boolean = false
    for(fileIndex <- 0 until fileSetLength-1){
      fileParam = fileSet(fileIndex)
      val file: File = request.body.asMultipartFormData.get.file(fileParam).get.ref.file
      val fileName: String = request.body.asMultipartFormData.get.file(fileParam).get.filename
      fileObjectList.add(FileObject(fileName,file))
          }
    validFileExtn = true
    (validFileExtn, fileObjectList)
  }

	def checkFileType(processor: StaxProcessor, fileName: String)
                   (implicit scheme: String, hc: HeaderCarrier, request: RequestWithOptionalEmpRef[_], messages: Messages):ListBuffer[SheetErrors] = {
    if (!uploadedFileUtil.checkODSFileType(fileName)) {
      throw ERSFileProcessingException(
        Messages("ers_check_file.file_type_error", fileName),
        Messages("ers_check_file.file_type_error", fileName))
    }
    parseOdsContent(processor, fileName)(scheme, hc, request, messages)
  }

  def parseOdsContent(processor: StaxProcessor, uploadedFileName: String)
										 (implicit scheme: String, hc : HeaderCarrier, request: RequestWithOptionalEmpRef[_], messages: Messages): ListBuffer[SheetErrors] = {
    DataGenerator.getErrors(processor, scheme, uploadedFileName)
  }
}
