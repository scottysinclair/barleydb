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

import com.smartstream.mi.model.XmlStructure;

public class QXMLStructure extends QueryObject<XmlStructure> {
    private static final long serialVersionUID = 1L;

    public QXMLStructure() {
        super(XmlStructure.class);
    }

    public QXMLStructure(QueryObject<?> parent) {
        super(XmlStructure.class, parent);
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }
}
