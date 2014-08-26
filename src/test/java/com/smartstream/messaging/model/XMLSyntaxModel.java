package com.smartstream.messaging.model;

import java.util.List;

public interface XMLSyntaxModel extends SyntaxModel {
	
	List<XMLMapping> getMappings();
	
	XMLStructure getStructure();
	
	void setStructure(XMLStructure xmlStructure);
}