package com.smartstream.messaging.query;

import com.smartstream.mac.query.QUser;
import com.smartstream.messaging.model.*;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;


public class QXMLSyntaxModel extends QueryObject<XMLSyntaxModel> {
	private static final long serialVersionUID = 1L;

    public QXMLSyntaxModel() {
		this(null);
	}

	public QXMLSyntaxModel(QueryObject<?> parent) {
		super(XMLSyntaxModel.class, parent);
	}

	public QXMLSyntaxModel disableName() {
	    addDisabled("name");
	    return this;
	}

	public QXMLMapping joinToMappings() {
		QXMLMapping mapping = new QXMLMapping();
		addJoin(mapping, "mappings");
		return mapping;
	}
	public QUser joinToUser() {
		QUser user = new QUser();
		addJoin(user, "user");
		return user;
	}
	public QXMLStructure joinToStructure() {
		QXMLStructure structure = new QXMLStructure(this);
		addJoin(structure, "structure");
		return structure;
	}
	public QXMLMapping existsMapping() {
		QXMLMapping mapping = new QXMLMapping(this);
		addExists(mapping, "mappings");
		return mapping;
	}
	/*
	public QXMLMapping existsParentMapping() {
		QXMLMapping mapping = new QXMLMapping(this);
		addExists(mapping, "mappings");
		return mapping;
	}
	*/
	public QUser existsUser() {
		QUser user = new QUser(this);
		addExists(user, "user");
		return user;
	}
	public QXMLStructure existsStructure() {
		QXMLStructure structure = new QXMLStructure(this);
		addExists(structure, "structure");
		return structure;
	}
	public QProperty<SyntaxType> syntaxType() {
		return new QProperty<SyntaxType>(this, "syntaxType");
	}
	public QProperty<String> syntaxName() {
		return new QProperty<String>(this, "name");
	}

}