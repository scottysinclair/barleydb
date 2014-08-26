package com.smartstream.messaging.model;

public interface CsvStructureField {
	
	Long getId();
	
	String getName();
	
	void  setName(String name);
	
	Integer getColumnIndex();
	
	void setColumnIndex(Integer columnIndex);
	
	Boolean getOptional();
	
	void setOptional(Boolean optional);
	
	CsvStructure getStructure();
	
	void setStructure(CsvStructure structure);
	
}
