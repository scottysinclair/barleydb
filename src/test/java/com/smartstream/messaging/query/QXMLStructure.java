package com.smartstream.messaging.query;

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

import com.smartstream.messaging.model.XMLStructure;

public class QXMLStructure extends QueryObject<XMLStructure> {
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
