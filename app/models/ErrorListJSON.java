package models;


import java.util.TreeMap;

public class ErrorListJSON {

	private TreeMap<String, SheetJSON> errorMap = new TreeMap<String,SheetJSON>();
	private String totalErrorCount;
	
	public ErrorListJSON(String totalErrorCount){
		this.totalErrorCount = totalErrorCount;
	}
	
	public String getTotalErrorCount() {
		return totalErrorCount;
	}

	public TreeMap<String, SheetJSON> getErrorMap() {
		return errorMap;
	}
	
	public void addSheet(SheetJSON sheetJSON){
		this.errorMap.put(sheetJSON.getNumber(), sheetJSON);	
	}
}
