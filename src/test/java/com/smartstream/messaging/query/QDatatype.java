package com.smartstream.messaging.query;

import com.smartstream.messaging.model.Datatype;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

public class QDatatype extends QueryObject<Datatype> {

	private static final long serialVersionUID = 1L;

	public QDatatype() {
		super(Datatype.class);
	}

	public QProperty<String> name() {
		return new QProperty<String>(this, "name");
	}

}
