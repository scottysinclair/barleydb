package com.smartstream.messaging.query;

import com.smartstream.messaging.model.XMLStructure;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;


public class QXMLStructure extends QueryObject<XMLStructure>{
	private static final long serialVersionUID = 1L;

    public QXMLStructure() {
		super(XMLStructure.class);
	}
	public QXMLStructure(QueryObject<?> parent) {
		super(XMLStructure.class, parent);
	}

	public QProperty<String> name() {
		return new QProperty<String>(this, "name");
	}
}
