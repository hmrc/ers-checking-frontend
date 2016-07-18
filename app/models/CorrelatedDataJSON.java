package models;

import java.util.ArrayList;
import java.util.List;

import models.ErrorTypeCountJSON;

public class CorrelatedDataJSON {
	private ArrayList<ErrorTypeCountJSON>  errorTypeList;
	
	public CorrelatedDataJSON() {
		errorTypeList = new ArrayList<ErrorTypeCountJSON>();
	}
	
	public void putCorrelatedData(ErrorTypeCountJSON errorCountType){
		errorTypeList.add(errorCountType);
	}
	
	public List<ErrorTypeCountJSON> getAllCorrelatedData(){
		return this.errorTypeList;
	}
}
