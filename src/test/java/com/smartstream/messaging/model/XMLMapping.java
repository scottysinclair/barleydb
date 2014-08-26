package com.smartstream.messaging.model;

public interface XMLMapping {
	
	Long getId();
	
	void setSyntaxModel(XMLSyntaxModel syntaxModel);

	XMLSyntaxModel getSyntaxModel();
	
	void setSubSyntaxModel(XMLSyntaxModel syntaxModel);

	XMLSyntaxModel getSubSyntaxModel();
	
	void setXpath(String xpath);
	
	String getXpath();
	
	void setTarget(String target);
	
	String getTarget();
}