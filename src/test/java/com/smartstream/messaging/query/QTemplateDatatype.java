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

import scott.sort.api.query.QueryObject;

import com.smartstream.messaging.model.Template;

public class QTemplateDatatype extends QueryObject<Template> {

    private static final long serialVersionUID = 1L;

    public QTemplateDatatype(QueryObject<?> parent) {
        super(null, "com.smartstream.messaging.TemplateDatatype", parent);

    }

    public QTemplateDatatype() {
        super(null, "com.smartstream.messaging.TemplateDatatype", null);
    }

    public QDatatype joinToDatatype() {
        QDatatype datatype = new QDatatype();
        addLeftOuterJoin(datatype, "datatype");
        return datatype;
    }
}
