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

//	public String getErrorName() {
//		return CellError.values()[Integer.parseInt(error) - 1].getSingularErrorName();
//	}
//
//	public String getErrorDescription(){
//
//		return CellError.values()[Integer.parseInt(error)-1].getErrorDescription();
//	}

	@Override
	public int compareTo(final CellErrorJSON e) {
		final int thisCol = Integer.parseInt(this.column);
		final int theirCol = Integer.parseInt(e.getColumn());
		//        int thisRow = Integer.parseInt(this.row);
		//        int theirRow =
		//
		if(thisCol < theirCol){
			return -1;
		}else if(thisCol > theirCol){
			return 1;
		}else{
			return 0;
		}
	}
}
