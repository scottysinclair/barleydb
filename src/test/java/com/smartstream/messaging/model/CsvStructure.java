package com.smartstream.messaging.model;

import java.util.List;

public interface CsvStructure {
	
	Long getId();
	
	String getName();
	
	void  setName(String name);
	
	List<CsvStructureField> getFields();
	
}
