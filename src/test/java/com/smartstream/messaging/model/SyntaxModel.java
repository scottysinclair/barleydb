package com.smartstream.messaging.model;

import com.smartstream.mac.model.User;

public interface SyntaxModel {

	Long getId();
	
	String getName();
	
	void setName(String name);
	
	SyntaxType getSyntaxType();

	void setSyntaxType(SyntaxType syntaxType);
	
	User getUser();
	
	void setUser(User user);

}
