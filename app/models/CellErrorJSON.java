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

package models;

public class CellErrorJSON implements Comparable<CellErrorJSON> {

	private String column = "";
	private String row = "";
	private String error = "";

	public CellErrorJSON(final String column, final String row, final String error) {
		this.column = column;
		this.row = row;
		this.error = error;
	}

	public String getColumn() {
		return column;
	}

	public String getRow() {
		return row;
	}


	@Override
	public int compareTo(final CellErrorJSON e) {
		final int thisCol = Integer.parseInt(this.column);
		final int theirCol = Integer.parseInt(e.getColumn());

		if(thisCol < theirCol){
			return -1;
		}else if(thisCol > theirCol){
			return 1;
		}else{
			return 0;
		}
	}
}
