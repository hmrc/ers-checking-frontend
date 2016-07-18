package models;

public class ErrorTypeCountJSON {
	private final String errorCode;
	private final String errorCount;

	public ErrorTypeCountJSON(final String errorNumber, final String errorCount){
		this.errorCode = errorNumber;
		this.errorCount = errorCount;
	}

	public String getErrorCode(){
		return this.errorCode;
	}

	public String getErrorCount(){
		return this.errorCount;
	}

//	public String getPluralErrorName() {
//		return CellError.values()[Integer.parseInt(errorCode) - 1].getPluralErrorName();
//	}
//
//	public String getSingularPluralName() {
//		return CellError.values()[Integer.parseInt(errorCode) - 1].getSingularErrorName();
//	}
}
