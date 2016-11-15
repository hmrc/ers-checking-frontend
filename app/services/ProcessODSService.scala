/*
 * Copyright 2016 HM Revenue & Customs
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

import java.io.{File, InputStream}
import java.util.ArrayList
import java.util.zip.ZipFile
import models.{ERSFileProcessingException, SheetErrors}
import play.api.{Logger, Play}
import models.FileObject
import play.api.Play.current
import play.api.i18n.Messages
import play.api.libs.Files
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{CacheUtil, UploadedFileUtil}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ListBuffer
import utils.ParserUtil
import play.api.i18n.Messages.Implicits._

object ProcessODSService extends ProcessODSService {
  override val uploadedFileUtil: UploadedFileUtil = UploadedFileUtil
  override val cacheUtil:CacheUtil = CacheUtil
}

trait ProcessODSService {
  val uploadedFileUtil: UploadedFileUtil
  val cacheUtil:CacheUtil

  def performODSUpload()(implicit request: Request[AnyContent],scheme:String, authContext : AuthContext, hc : HeaderCarrier, messages: Messages):Boolean = {
    val spreadSheetFile = request.body.asMultipartFormData.get.file("fileUpload")
    spreadSheetFile.map { file =>
      val errorList: ListBuffer[SheetErrors] = checkFileType(file)(scheme, authContext,hc ,request, messages)
      val fileName: String = request.body.asMultipartFormData.get.file("fileUpload").get.filename
      cacheUtil.cache[String](CacheUtil.FILE_NAME_CACHE, fileName).recover {
        case e: Exception => {
          Logger.error("ODSSaveFileName: Unable to save File Name. Error: " + e.getMessage)
          throw e
        }
      }
      ParserUtil.isFileValid(errorList, "performODSUpload")
    }.getOrElse {
      throw ERSFileProcessingException(messages("ers_check_file.no_file_error"),messages("ers_check_file.no_file_error"), needsExtendedInstructions = true)
    }
  }

  def createFileObject(uploadedFile: Seq[MultipartFormData.FilePart[Files.TemporaryFile]])(implicit request: Request[AnyContent]): (Boolean, ArrayList[FileObject]) = {
    val fileSet = uploadedFile.map(file => file.key)
    val fileSetLength = fileSet.length
    val fileObjectList = new java.util.ArrayList [FileObject] (fileSetLength)
    var fileParam:String = ""
    var validFileExtn: Boolean = false
    for(fileIndex <- 0 to fileSetLength-1){
      fileParam = fileSet(fileIndex)
      val file: File = request.body.asMultipartFormData.get.file(fileParam).get.ref.file;
      val fileName: String = request.body.asMultipartFormData.get.file(fileParam).get.filename
      //validFileExtn = uploadedFileUtil.checkCSVFileType(fileName)
      fileObjectList.add(new FileObject(fileName,file))
          }
    validFileExtn = true
    (validFileExtn, fileObjectList)
  }

  def checkFileType(uploadedFile: MultipartFormData.FilePart[Files.TemporaryFile])(implicit scheme:String, authContext: AuthContext,hc: HeaderCarrier, request: Request[_], messages: Messages):ListBuffer[SheetErrors] = {
    if (!uploadedFileUtil.checkODSFileType(uploadedFile.filename)) {
      throw ERSFileProcessingException(messages("ers_check_file.file_type_error", uploadedFile.filename), messages("ers_check_file.file_type_error", uploadedFile.filename))
    }
    val res = parseOdsContent(uploadedFile.ref.file.getAbsolutePath, uploadedFile.filename)(scheme, authContext, hc, request)
    UploadedFileUtil.deleteFile(uploadedFile.ref.file)
    res
  }

  def parseOdsContent(fileName: String, uploadedFileName: String)(implicit scheme:String, authContext: AuthContext, hc : HeaderCarrier, request: Request[_]): ListBuffer[SheetErrors] = {

    val zipFile: ZipFile = new ZipFile(fileName)
    val content: InputStream = zipFile.getInputStream(zipFile.getEntry("content.xml"))
    val processor = new StaxProcessor(content)
    val result = DataGenerator.getErrors(processor, scheme, uploadedFileName)(authContext, hc, request, applicationMessages)
    zipFile.close()
    result
  }

}
