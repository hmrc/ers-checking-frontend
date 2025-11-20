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

package utils



trait CacheUtil {

  // Cache Keys
  val CALLBACK_DATA_KEY = "callback_data_key"
  val CALLBACK_DATA_KEY_CSV = "callback_data_key_csv"

  // Cache Ids
  val SCHEME_CACHE: String = "scheme-type"
  val FILE_TYPE_CACHE: String = "check-file-type"
  val SCHEME_ERROR_COUNT_CACHE: String = "scheme-error-count"
  val FILE_NAME_NO_EXTN_CACHE: String = "file-name-no-extn"
  val ERROR_LIST_CACHE: String = "error-list"
  val ERROR_SUMMARY_CACHE: String = "error-summary"
  val FORMAT_ERROR_CACHE: String = "format_error"
  val FORMAT_ERROR_CACHE_PARAMS: String = "format_error_params"
  val FORMAT_ERROR_EXTENDED_CACHE: String = "format_extended_error"
  val FILE_NAME_CACHE: String = "file-name"
  val CSV_FILES_UPLOAD: String = "csv-files-upload"

}
