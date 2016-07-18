package models;

import java.util.ArrayList;
import java.util.List;

public class SheetJSON {
	
	private String number = "";
	private String name = "";
	private String errorCount = "";
	private List<CellErrorJSON> errorList = new ArrayList<CellErrorJSON>();
	
	public SheetJSON(String number, String name, String errorCount){
		this.number = number;
		this.name = name;
		this.errorCount = errorCount;
	}
	
	public void addError(CellErrorJSON cellErrorJSON){		
		if(!cellErrorJSON.getRow().equals("0"))			
			this.errorList.add(cellErrorJSON);
	}
	
	public String getNumber() {
		return number;
	}

	public String getName() {
		return name;
	}

	public String getErrorCount() {
		return errorCount;
	}

	public List<CellErrorJSON> getErrorList() {
		return errorList;
	}
}
