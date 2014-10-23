package com.smartstream.mi.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;

import com.smartstream.mi.model.Datatype;

public class QDatatype extends QueryObject<Datatype> {

    private static final long serialVersionUID = 1L;

    public QDatatype() {
        super(Datatype.class);
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }

}
