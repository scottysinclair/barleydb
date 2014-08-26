package com.smartstream.messaging.model;

import java.util.List;

public interface CsvSyntaxModel extends SyntaxModel {

	List<CsvMapping> getMappings();
	
	CsvStructure getStructure();
	
	void setStructure(CsvStructure csvStructure);

}