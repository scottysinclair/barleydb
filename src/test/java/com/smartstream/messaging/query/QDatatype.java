package com.smartstream.messaging.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;

import com.smartstream.messaging.model.Datatype;

public class QDatatype extends QueryObject<Datatype> {

    private static final long serialVersionUID = 1L;

    public QDatatype() {
        super(Datatype.class);
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }

}
